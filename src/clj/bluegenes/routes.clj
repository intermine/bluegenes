(ns bluegenes.routes
  (:require [compojure.core :refer [GET POST defroutes context]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.ids :as ids]
            [bluegenes.index :as index]
            [ring.middleware.params :refer [wrap-params]]))

; Define the top level URL routes for the server
(defroutes routes
  ;;serve compiled files, i.e. js, css, from the resources folder
  (resources "/")

  (GET "/version" [] (response {:version "0.1.0"}))

  ; Anything within this route is the API web service:
  (context "/api" []
    (context "/auth" [] auth/routes)
    (context "/ids" [] ids/routes))

  (GET "*" [] (index/index))
  (wrap-params (POST "*" req (index/index req))))
