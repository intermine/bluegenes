(ns bluegenes.ws.auth
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [imcljs.auth :as im-auth]
            [clojure.string :refer [blank?]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :as response]

            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])

            [cemerick.url :as url]

            [friend-oauth2.workflow :as oauth2]
            [friend-oauth2.util :refer [format-config-uri]]



            ))

(defn credential-fn
  [token]
  ;;lookup token in DB or whatever to fetch appropriate :roles
  {:identity token :roles #{::user}})

(def client-config
  {:client-id ""
   :client-secret ""
   :callback {:domain "http://localhost:8080" :path "/api/auth/oauth2/google/oauth2callback"}})

(def uri-config
  {:authentication-uri {:url "https://accounts.google.com/o/oauth2/auth"
                        :query {:client_id (:client-id client-config)
                                :response_type "code"
                                :redirect_uri (format-config-uri client-config)
                                :scope "email"}}

   :access-token-uri {:url "https://accounts.google.com/o/oauth2/token"
                      :query {:client_id (:client-id client-config)
                              :client_secret (:client-secret client-config)
                              :grant_type "authorization_code"
                              :redirect_uri (format-config-uri client-config)}}})

(defn fetch-token
  [{:keys [service username password token] :as im}]
  (im-auth/basic-auth service username password))

(defn fetch-identity
  [{:keys [service token]}]
  (im-auth/who-am-i? service token))

(defn logout
  "Log the user out by clearing the session"
  []
  (-> (response/ok {:success true}) (assoc :session nil)))

(defn handle-auth
  "Ring handler for handling authentication. Attempts to authenticate with the
  IM server (via web services) by fetching a token. If successful, return
  the token and store it in the session."
  [{{username :username password :password} :params :as req}]
  ; clj-http throws exceptions for 'bad' responses:
  (try
    ; Try to fetch a token from the IM server web service
    ; using the IM_SERVICE env/config variable as the remote host
    (let [token             (fetch-token {:username username
                                          :password password
                                          :service {:root (:im-service env)}})
          whoami            (im-auth/who-am-i? {:root (:im-service env) :token token} token)
          whoami-with-token (assoc whoami :token token)]
      ; Store the token in the session and return it to the user
      (->
        (response/ok whoami-with-token)
        (assoc :session {:identity whoami-with-token})))
    (catch Exception e
      (let [{status :status body :body :as error} (ex-data e)]
        (println "Authentication Error:")
        (clojure.pprint/pprint (ex-data e))
        ; Parse the body of the bad request sent back from the IM server
        (let [json-response (cheshire/parse-string body)]
          (case status
            401 (response/unauthorized json-response)
            500 (response/internal-server-error json-response)
            (response/not-found {:stack-trace error
                                 :error "Unable to reach remote server"})))))))



(defroutes routes
           (GET "/logout" session (logout))
           (POST "/login" session handle-auth)
           (GET "/session" session (response/ok (:session session)))
           (GET "/oauth2/google" req (do
                                       (pprint req)
                                       (response/ok "done")))
           (GET "/oauth2/google-callback" req (do
                                       (pprint req)
                                       (response/ok "done"))))

(def secured-routes
  (friend/authenticate
    routes
    {:allow-anon? true
     :workflows [(oauth2/workflow
                   {:client-config client-config
                    :uri-config uri-config
                    :credential-fn credential-fn})]}))