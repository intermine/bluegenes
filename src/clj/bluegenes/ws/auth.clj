(ns bluegenes.ws.auth
  (:require [compojure.core :refer [GET POST defroutes]]
            [imcljs.auth :as im-auth]
            [cheshire.core :as cheshire]
            [ring.util.http-response :as response]))

(defn logout
  "Log the user out by clearing the session"
  []
  (-> (response/ok {:success true})
      (assoc :session nil)))

(defn handle-auth
  "Ring handler for handling authentication. Attempts to authenticate with the
  IM server (via web services) by fetching a token. If successful, return
  the token and store it in the session."
  [{{username :username password :password service :service mine-id :mine-id} :params :as req}]
  ; clj-http throws exceptions for 'bad' responses:
  (try
    ; Try to fetch a token from the InterMine server web service
    (let [token (im-auth/basic-auth service username password)
          ; Use the the token to resolve the user's identity
          whoami (im-auth/who-am-i? (assoc service :token token) token)
          ; Build an identity map (token, mine-id, whoami information)
          whoami-with-token (assoc whoami :token token :mine-id (name mine-id))]
      ; Store the identity map in the session and return it to the user:
      (->
        (response/ok whoami-with-token)
        (assoc :session {:identity whoami-with-token})))
    (catch Exception e
      (let [{status :status body :body :as error} (ex-data e)]
        ; Parse the body of the bad request sent back from the IM server
        (let [json-response (cheshire/parse-string body)]
          (case status
            401 (response/unauthorized json-response)
            500 (response/internal-server-error json-response)
            (response/not-found {:stack-trace error
                                 :error "Unable to reach remote server"})))))))

(defroutes routes
           (GET "/logout" session (logout))
           (POST "/login" session handle-auth))