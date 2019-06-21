(ns bluegenes.handler
  (:require [bluegenes.routes :as routes]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [compojure.core :refer [routes]]
            [bluegenes-tool-store.core :as tool]))

(def combined-routes
  (routes routes/routes tool/routes))

(def handler (-> #'combined-routes
                 ; Watch changes to the .clj and hot reload them
                 wrap-reload
                 ; Add session functionality to the Ring requests
                 wrap-session
                 ; Accept and parse request parameters in various formats
                 (wrap-restful-format :formats [:json :json-kw :transit-msgpack :transit-json])))
