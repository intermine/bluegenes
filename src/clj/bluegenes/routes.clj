(ns bluegenes.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources]]
            [bluegenes.index :as index]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.tools :as tools]
            [bluegenes.ws.mymine :as mymine]
            ))

(defroutes routes
           (GET "/" req
             ; If the user has already logged in then pass their identity
             ; into the constructor of the BlueGenes javascript
             (index/index (:identity (:session req))))
           (resources "/")
           (GET "/version" [] (response {:version "0.1.0"}))
           (context "/api" []
             (context "/auth" [] auth/routes)
             (context "/tools" [] tools/routes)
             (context "/mymine" [] mymine/routes)))