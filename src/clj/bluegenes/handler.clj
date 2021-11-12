(ns bluegenes.handler
  (:require [bluegenes.routes :refer [routes]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.reload :refer [wrap-reload]]
            [compojure.middleware :refer [wrap-canonical-redirect]]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [config.core :refer [env]]))

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

(def handler (-> #'routes
                 ;; Watch changes to the .clj and hot reload them
                 (cond-> (:development env) (wrap-reload {:dirs ["src/clj"]}))
                 ;; Add session functionality
                 ;; SameSite CANNOT be Strict, as this would cause Firefox to
                 ;; NOT include the cookie when redirected back to Bluegenes
                 ;; from an OAuth2 provider.
                 (wrap-session {:cookie-attrs {:same-site :lax :http-only true}})
                 ;; Merges request :body-params into :params
                 (wrap-params)
                 ;; Decodes requests and encodes responses based on headers.
                 ;; Primarily to take transit+json body from frontend and put
                 ;; into :body-params.
                 (wrap-format)
                 ;; Redirect to the correct route when trailing slash is present in path.
                 ;; This is for the frontend router, which doesn't handle trailing slashes.
                 (wrap-canonical-redirect remove-trailing-slash)))
