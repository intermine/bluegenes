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
            [compojure.middleware :refer [wrap-canonical-redirect]]
            [bluegenes-tool-store.core :as tool]))

(defn remove-trailing-slash
  "Remove the trailing '/' from a URI string, if it exists and isn't the only character."
  [^String uri]
  (if (.endsWith uri "/")
    (let [len (.length uri)]
      ;; Without this check, the response becomes 301 with a blank Location
      ;; header for request uri path "/", causing nothing to be loaded.
      (if (> len 1)
        (.substring uri 0 (dec len))
        uri))
    uri))

(def combined-routes
  (routes tool/routes routes/routes))

(def handler (-> #'combined-routes
                 ; Watch changes to the .clj and hot reload them
                 wrap-reload
                 ; Add session functionality to the Ring requests
                 wrap-session
                 ; Accept and parse request parameters in various formats
                 (wrap-restful-format :formats [:json :json-kw :transit-msgpack :transit-json])
                 ; Redirect to the correct route when trailing slash is present in path.
                 ; This is for the frontend router, which doesn't handle trailing slashes.
                 (wrap-canonical-redirect remove-trailing-slash)))
