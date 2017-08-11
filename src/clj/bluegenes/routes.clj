(ns bluegenes.routes
  (:require [compojure.core :refer [GET defroutes context]]
            [compojure.route :refer [resources]]
            [bluegenes.index :as index]
            [ring.util.response :refer [response]]
            [bluegenes.ws.auth :as auth]))

(defroutes routes
           (GET "/" [] (index/index))
           (resources "/")
           (GET "/version" [] (response {:version "0.1.0"}))
           (context "/api" []
             (context "/auth" [] auth/routes)))