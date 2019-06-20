(ns bluegenes.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources files]]
            [bluegenes.index :as index]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.tools :as tools]
            [config.core :refer [env]]
            [bluegenes.ws.mymine :as mymine]
            [bluegenes.ws.ids :as ids]))

; Define the top level URL routes for the server
(defroutes routes
  (GET "/" req
             ; The user might have an active session. Pass their identity to the client to automatically
             ; log the user into the application:

    (index/index (:identity (:session req))))

             ;;serve compiled files, i.e. js, css, from the resources folder
  (resources "/")

             ;; serve all tool files in bluegenes/tools automatically.
             ;; they can't go in the resource folder b/c then they get jarred
             ;; when running uberjar or clojar targets,
             ;; and make the jars about a million megabytes too big.

  (files "/tools" {:root (:bluegenes-tool-path env), :allow-symlinks? true})

  (GET "/version" [] (response {:version "0.1.0"}))

           ; Anything within this route is the API web service:
  (context "/api" []
    (context "/auth" [] auth/routes)
    (context "/tools" [] tools/routes)
    (context "/mymine" [] mymine/routes)
    (context "/ids" [] ids/routes)))
