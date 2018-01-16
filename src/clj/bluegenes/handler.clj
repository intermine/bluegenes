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

(def handler (-> #'routes/routes
                 ; Watch changes to the .clj and hot reload them
                 wrap-reload
                 ; Add session functionality to the Ring requests
                 wrap-session
                 ; Accept and parse request parameters in various formats
                 (wrap-restful-format :formats [:json :json-kw :transit-msgpack :transit-json])
                 ; The rest are mostly replaced by wrap-restful-format but are being left for historical purposes:
                 ;wrap-params
                 ;wrap-restful-format
                 ;wrap-json-response
                 ;wrap-keyword-params
                 ;wrap-params
                 ))
