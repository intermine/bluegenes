(ns bluegenes.handler
  (:require [compojure.core :refer [GET defroutes]]
            [bluegenes.routes :as routes]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.format :refer [wrap-restful-format]]))


(def dev-handler (-> #'routes/routes
                     wrap-session
                     wrap-reload
                     wrap-json-response
                     wrap-keyword-params
                     wrap-params))

(def handler (-> #'routes/routes
                 wrap-reload
                 wrap-session
                 (wrap-restful-format :formats [:json :json-kw :transit-msgpack :transit-json])
                 ;wrap-params
                 ;wrap-restful-format
                 ;wrap-json-response
                 ;wrap-keyword-params
                 ;wrap-params
                 ))
