(ns bluegenes.ws.auth
  (:require [compojure.core :refer [POST defroutes GET]]
            [imcljs.auth :as im-auth]
            [cheshire.core :as cheshire]
            [ring.util.http-response :as response]
            [taoensso.timbre :as timbre]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

;; You may notice that we keep session data on the backend, although this data
;; is currently not used (except for OAuth 2.0, which only depends on the
;; session data added in `oauth2authenticator`). This is because the frontend
;; does most of the requests by itself. There are some trade-offs with this:
;;
;; - For most requests, an OPTIONS preflight request has to be sent prior to
;; the actual request, incurring a performance penalty (hopefully the majority
;; of BlueGenes instances will be hosted on the same domain as the InterMine
;; instances they'll use the most, avoiding the preflight request)
;;
;; - The auth data stored to localStorage can be read by nefarious JS code that
;; sneaks in (XSS; one such path is by installing a malicious BlueGenes tool)
;;
;; The obvious alternative would be to only have the BlueGenes backend handle
;; authentication by using sessions. However, this would mean that all requests
;; would have to be proxied by the backend, (essentially adding a detour to
;; every request) another big performance penalty (although this would avoid
;; preflight requests completely).
;;
;; The optimal solution would be to have each InterMine instance handle
;; sessions themselves, not requiring that BlueGenes keep any authentication
;; data. Was this approach tried before and deemed too difficult? Or was the
;; "client manages token" approach chosen by default for historical reasons, as
;; each API client has been doing this. At some point we might raise this
;; question again...

(defn logout
  "Log the user out by clearing the session. Also sends a request to the InterMine
  instance to invalidate the token."
  [{{:keys [service]} :params :as _req}]
  (try
    ;; Might throw if we're missing token (401) or if the InterMine instance
    ;; is older and doesn't have the service implemented (invalid response).
    (im-auth/logout service)
    (-> (response/ok {:success true})
        (assoc :session nil))
    (catch Exception _
      (-> (response/ok {:success true})
          (assoc :session nil)))))

(defn login
  "Login using the new login service, with fallback to basic auth."
  [service username password]
  (let [res (im-auth/login service username password)]
    (if (= (:status res) 302)
      ;; The InterMine instance doesn't support the new login service.
      ;; Fall back to basic auth.
      (im-auth/basic-auth service username password)
      (get-in res [:output :token]))))

(defn handle-auth
  "Ring handler for handling authentication. Attempts to authenticate with the
  IM server (via web services) by fetching a token. If successful, return
  the token and store it in the session."
  [{{:keys [username password service mine-id]} :params :as _req}]
  ; clj-http throws exceptions for 'bad' responses:
  (try
    ; Try to fetch a token from the InterMine server web service
    (let [token (login service username password)
          ; Use the the token to resolve the user's identity
          whoami (im-auth/who-am-i? (assoc service :token token) token)
          ; Build an identity map (token, mine-id, whoami information)
          whoami-with-token (assoc whoami :token token :mine-id (name mine-id))]
      ; Store the identity map in the session and return it to the user:
      (->
       (response/ok whoami-with-token)
       (assoc :session {:identity whoami-with-token})))
    (catch Exception e
      (let [{:keys [status body] :as error} (ex-data e)]
        ; Parse the body of the bad request sent back from the IM server
        (let [json-response (cheshire/parse-string body)]
          (case status
            401 (response/unauthorized json-response)
            500 (response/internal-server-error json-response)
            (response/not-found {:stack-trace error
                                 :error "Unable to reach remote server"})))))))

(defn register
  "Ring handler for registering a new user with the IM server. If successful,
  perform a regular login to get a proper token and store it in the session."
  [{{:keys [username password service mine-id]} :params :as _req}]
  ; clj-http throws exceptions for 'bad' responses:
  (try
    ;; Registration only returns a temporaryToken valid for 24 hours.
    (im-auth/register service username password)
    ;; We call the login service directly after so we can get a proper token.
    (let [token (login service username password)
          ; Use the the token to resolve the user's identity
          whoami (im-auth/who-am-i? (assoc service :token token) token)
          ; Build an identity map (token, mine-id, whoami information)
          whoami-with-token (assoc whoami :token token :mine-id (name mine-id))]
      ; Store the identity map in the session and return it to the user:
      (-> (response/ok whoami-with-token)
          (assoc :session {:identity whoami-with-token})))
    (catch Exception e
      (let [{:keys [status body] :as error} (ex-data e)
            json-response (cheshire/parse-string body)]
        ; Parse the body of the bad request sent back from the IM server
        (case status
          400 (response/bad-request json-response)
          401 (response/unauthorized json-response)
          500 (response/internal-server-error json-response)
          (response/not-found {:stack-trace error
                               :error "Unable to reach remote server"}))))))

(defn oauth2authenticator
  [{{:keys [service mine-id provider]} :params :as _req}]
  (try
    (-> (response/ok (im-auth/oauth2authenticator service provider))
        (assoc :session {:service service
                         :mine-id mine-id}))
    (catch Exception e
      ;; Forward the error response to client so it can handle it.
      (ex-data e))))

;; TODO remove logging
;; TODO pass renamed list data to session.init as well
(defn oauth2callback
  [{{:keys [provider state code]} :params
    {:keys [service mine-id]} :session}]
  (try
    (let [res (im-auth/oauth2callback service {:provider provider :state state :code code})
          token (get-in res [:output :token])]
      (timbre/infof "Got oauth2callback response: %s" (pr-str res))
      (-> (response/found (str "/" mine-id))
          (assoc :session {:identity (assoc service :token token)
                           :init {:token token}})))
    (catch Exception e
      (timbre/errorf "oauth2callback error: %s" (pr-str (ex-data e)))
      (let [{:keys [body]} (ex-data e)
            {:keys [error]} (cheshire/parse-string body true)]
        (-> (response/found (str "/" mine-id))
            (assoc :session {:init {:events [[:messages/add
                                              {:style "warning"
                                               :markup (str "Failed to login using OAuth 2.0"
                                                          (when (not-empty error)
                                                            (str ": " error)))}]]}}))))))

(defroutes routes
  (POST "/logout" [] logout)
  (POST "/login" [] handle-auth)
  (POST "/register" [] register)
  (POST "/oauth2authenticator" [] oauth2authenticator)
  (wrap-params
   (wrap-keyword-params
    (GET "/oauth2callback" [] oauth2callback))))
