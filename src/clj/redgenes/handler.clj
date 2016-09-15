(ns redgenes.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]))

(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (resources "/")
  (context "/api" [] webservice/routes))

(def dev-handler (-> #'routes wrap-reload))

(def handler routes)
