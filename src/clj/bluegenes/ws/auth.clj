(ns bluegenes.ws.auth
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [bluegenes.auth :as auth]
            [clojure.string :refer [blank?]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [ring.util.http-response :as response]))

(defn logout
  "Log the user out by clearing the session"
  []
  (-> (response/ok {:success true}) (dissoc :session)))

(defn handle-auth
  "Ring handler for handling authentication. Attempts to authenticate with the
  IM server (via web services) by fetching a token. If successful, return
  the token and store it in the session."
  [{{username :username password :password} :params :as req}]
  ; clj-http throws exceptions for 'bad' responses:
  (try
    ; Try to fetch a token from the IM server web service
    ; using the IM_SERVICE env/config variable as the remote host
    (let [token (auth/fetch-token {:username username
                                   :password password
                                   :service-str (:im-service env)})]
      ; Store the token in the session and return it to the user
      (-> (response/ok {:token token}) (assoc :session {:token token})))
    (catch Exception e
      (let [{status :status body :body :as error} (ex-data e)]
        ; Parse the body of the bad request sent back from the IM server
        (let [json-response (cheshire/parse-string body)]
          (case status
            401 (response/unauthorized json-response)
            500 (response/internal-server-error json-response)
            (response/not-found {:error "Unable to reach remote server"})))))))

(defroutes routes
           (GET "/logout" session (logout))
           (GET "/test" session handle-auth)
           (GET "/session" session (response/ok (:session session))))