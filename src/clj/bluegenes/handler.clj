(ns bluegenes.handler
  (:require [compojure.core :refer [GET defroutes]]
            [bluegenes.routes :as api]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.reload :refer [wrap-reload]]))


(def dev-handler (-> #'api/routes wrap-reload wrap-json-response wrap-params))

(def handler (-> #'api/routes wrap-json-response wrap-params))
