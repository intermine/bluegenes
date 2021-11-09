(ns bluegenes.ws.auth
  (:require [compojure.core :refer [POST defroutes GET]]
            [imcljs.auth :as im-auth]
            [cheshire.core :as cheshire]
            [ring.util.http-response :as response]
            [taoensso.timbre :as timbre]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [config.core :refer [env]]))

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
;; ^ not possible right now; JSESSIONID only works for JSP webapp, not WS requests
;; => Keep it as it is!

(defn use-backend-service
  "Substitute service root for the one the backend is configured to use, if it
  is so configured and matches the equivalent service root for the frontend.
  The latter check is required as we could be connecting to an external mine."
  [service]
  (let [{default :bluegenes-default-service-root
         backend :bluegenes-backend-service-root} env
        backend (not-empty backend)]
    (cond
      (nil? backend) service
      (= (:root service) default) (assoc service :root backend)
      :else service)))

(defn logout
  "Log the user out by clearing the session. Also sends a request to the InterMine
  instance to invalidate the token."
  [{{:keys [service]} :params :as _req}]
  (let [service (use-backend-service service)]
    (try
      ;; Might throw if we're missing token (401) or if the InterMine instance
      ;; is older and doesn't have the service implemented (invalid response).
      (im-auth/logout service)
      (-> (response/ok {:success true})
          (assoc :session nil))
      (catch Exception _
        (-> (response/ok {:success true})
            (assoc :session nil))))))

(defn login
  "Login using the new login service, with fallback to basic auth."
  [service username password]
  (let [res (im-auth/login service username password)]
    (if (= (:status res) 302)
      ;; The InterMine instance doesn't support the new login service.
      ;; Fall back to basic auth.
      (let [token (im-auth/basic-auth service username password)
            user (im-auth/who-am-i? (assoc service :token token) token)]
        [(assoc user :token token)])
      (let [{:keys [renamedLists user token]} (:output res)]
        [(assoc user :token token) renamedLists]))))

(defn handle-auth
  "Ring handler for handling authentication. Attempts to authenticate with the
  IM server (via web services) by fetching a token. If successful, return
  the token and store it in the session."
  [{{:keys [username password service]} :params :as _req}]
  (let [service (use-backend-service service)]
    (try
      (let [[user+token renamedLists] (login service username password)]
        (-> (response/ok {:identity user+token
                          :renamedLists renamedLists})
            (assoc :session ^:recreate {:identity user+token})))
      (catch Exception e
        (let [{:keys [status] :as res} (ex-data e)]
          (if status
            res
            (response/internal-server-error {:error (ex-message e)})))))))

(defn register
  "Ring handler for registering a new user with the IM server. If successful,
  perform a regular login to get a proper token and store it in the session."
  [{{:keys [username password service]} :params :as _req}]
  (let [service (use-backend-service service)]
    (try
      ;; Registration only returns a temporaryToken valid for 24 hours.
      (im-auth/register service username password)
      ;; We call the login service directly after so we can get a proper token.
      (let [[user+token renamedLists] (login service username password)]
        ;; We're passing renamedLists although there shouldn't be any due to
        ;; being a new account.
        (-> (response/ok {:identity user+token
                          :renamedLists renamedLists})
            (assoc :session ^:recreate {:identity user+token})))
      (catch Exception e
        (let [{:keys [status] :as res} (ex-data e)]
          (if status
            res
            (response/internal-server-error {:error (ex-message e)})))))))

(defn oauth2authenticator
  [{{:keys [service mine-id provider redirect_uri]} :params :as _req}]
  (let [service (use-backend-service service)]
    (try
      (-> (response/ok (im-auth/oauth2authenticator service provider))
          (assoc :session {:service service
                           :mine-id mine-id
                           :redirect_uri redirect_uri}))
      (catch Exception e
        ;; Forward the error response to client so it can handle it.
        (ex-data e)))))

(defn oauth2callback
  [{{:keys [provider state code]} :params
    {:keys [service mine-id redirect_uri]} :session}]
  (try
    (let [res (im-auth/oauth2callback service {:provider provider :state state :code code :redirect_uri redirect_uri})
          {:keys [renamedLists user token]} (:output res)
          user+token (assoc user
                            :token token
                            :login-method :oauth2)]
      (-> (response/found (str (:bluegenes-deploy-path env) "/" mine-id))
          (assoc :session ^:recreate {:identity user+token
                                      :init {:identity user+token
                                             :renamedLists renamedLists}})))
    (catch Exception e
      (timbre/errorf "oauth2callback error: %s" (ex-message e))
      (let [{:keys [body]} (ex-data e)
            error (or (:error (cheshire/parse-string body true))
                      (ex-message e))]
        (-> (response/found (str (:bluegenes-deploy-path env) "/" mine-id))
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
