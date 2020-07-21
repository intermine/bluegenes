(ns bluegenes.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.ids :as ids]
            [bluegenes.ws.rss :as rss]
            [bluegenes.index :as index]))

; Define the top level URL routes for the server
(defroutes routes
  ;;serve compiled files, i.e. js, css, from the resources folder
  (resources "/")

  (GET "/version" [] (response {:version "0.1.0"}))

  ; Anything within this route is the API web service:
  (context "/api" []
    (context "/auth" [] auth/routes)
    (context "/ids" [] ids/routes)
    (context "/rss" [] rss/routes))

  (GET "*" [] (index/index)))
