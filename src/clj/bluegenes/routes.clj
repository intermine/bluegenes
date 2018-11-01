(ns bluegenes.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources]]
            [bluegenes.index :as index]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.mymine :as mymine]
            [bluegenes.ws.ids :as ids]))

; Define the top level URL routes for the server
(defroutes routes
  (GET "/" req
             ; The user might have an active session. Pass their identity to the client to automatically
             ; log the user into the application:
    (index/index (:identity (:session req))))
  (resources "/")
  (GET "/version" [] (response {:version "0.1.0"}))
           ; Anything within this route is the API web service:
  (context "/api" []
    (context "/auth" [] auth/routes)
    (context "/mymine" [] mymine/routes)
    (context "/ids" [] ids/routes)))