(ns bluegenes.routes
  (:require [compojure.core :as compojure :refer [GET POST defroutes context]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :as response :refer [response]]
            [ring.util.http-response :refer [found see-other]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.ids :as ids]
            [bluegenes.ws.rss :as rss]
            [bluegenes.ws.lookup :as lookup]
            [bluegenes.index :refer [index]]
            [config.core :refer [env]]
            [bluegenes.utils :refer [env->mines get-service-root]]
            [clj-http.client :as client]
            [bluegenes-tool-store.core :as tool]
            [hiccup.page :refer [html5]]
            [clojure.string :as str]))

(defn with-init
  "One of BlueGenes' web service could have added some data we want passed on
  to the frontend to session.init, in which case we make sure to pass it on
  and remove it (as it gets 'consumed') from the session."
  [options {{:keys [init] :as session} :session}]
  (-> (response (index init options))
      (response/content-type "text/html")
      ;; This is very important - without it Firefox will request the HTML
      ;; twice, messing up session.init!
      (response/charset "utf-8")
      (assoc :session (dissoc session :init))))

(defn get-favicon
  "Get a favicon for when one isn't configured."
  []
  (let [mine-favicon (str (get-service-root env) "/model/images/favicon.ico")]
    (if (-> (client/get mine-favicon)
            (get-in [:headers "Content-Type"])
            (= "image/x-icon"))
      (found mine-favicon)
      (found (str (:bluegenes-deploy-path env) "/favicon-fallback.ico")))))

(defn not-found-page [{:keys [request-method uri] :as _req}]
  (let [bg-path (or (:bluegenes-deploy-path env) "/")]
    (html5
     [:head
      [:title "Page Not Found"]
      [:style "h1{ font-size:80px; font-weight:800; text-align:center; font-family: 'Roboto', sans-serif; } h2 { font-size:25px; text-align:center; font-family: 'Roboto', sans-serif; margin-top:-40px; } p{ text-align:center; font-family: 'Roboto', sans-serif; font-size:12px; } .container { width:300px; margin: 0 auto; margin-top:15%; }"]]
     [:body
      [:div.container
       [:h1 "404"]
       [:h2 "Page Not Found"]
       [:p "This " [:strong (-> request-method name str/upper-case)] " request to " [:strong uri] " is not handled by the BlueGenes server, which is deployed to " [:strong bg-path] ". "
        [:a {:href bg-path} "Click here"] " to open BlueGenes."]]])))

; Define the top level URL routes for the server
(def routes
  (compojure/let-routes [mines (env->mines env)
                         favicon* (delay (get-favicon))]
    (context (:bluegenes-deploy-path env) []
      ;;serve compiled files, i.e. js, css, from the resources folder
      (resources "/")

      ;; The favicon is chosen from the following order of priority:
      ;; 1. `public/favicon.ico` being present as a resource (admin will have to add this).
      ;; 2. `/<mine>/model/images/favicon.ico` being present on the default mine.
      ;; 3. `public/favicon-fallback.ico` which is always present.
      ;; Hence it follows that the following route won't be matched if [1] is true.
      (GET "/favicon.ico" [] @favicon*)

      (GET "/version" [] (response {:version "0.1.0"}))

      tool/routes

      ;; Anything within this context is the API web service.
      (context "/api" []
        (context "/auth" [] auth/routes)
        (context "/ids" [] ids/routes)
        (context "/rss" [] rss/routes))

      ;; Linking in.
      ;; Handles both configured mines and the /query path.
      (wrap-params
       (wrap-keyword-params
        (apply compojure/routes
               (for [path (concat (map :namespace mines) ["query"])
                     :let [redirect-path (str (:bluegenes-deploy-path env) "/"
                                              (when-not (= path "query")
                                                path))]]
                 (context (str "/" path) []
                   (GET "/portal.do" {params :params}
                     (-> (found redirect-path)
                         (assoc :session {:init {:linkIn {:target :upload
                                                          :data params}}})))
                   (POST "/portal.do" {params :params}
                     (-> (see-other redirect-path)
                         (assoc :session {:init {:linkIn {:target :upload
                                                          :data params}}}))))))))

      ;; Dynamic routes for handling permanent URL resolution on configured mines.
      (apply compojure/routes
             (for [{mine-ns :namespace :as mine} mines]
               (context (str "/" mine-ns) []
                 (GET ["/:lookup" :lookup #"[^:/.]+:[^:/.]+(?:\.rdf)?"] [lookup]
                   (lookup/ws lookup mine)))))

      ;; Passes options to index for including semantic markup with HTML.
      (GET "/" []
        (partial with-init {:semantic-markup :home
                            :mine (first mines)}))
      (apply compojure/routes
             (for [{mine-ns :namespace :as mine} mines]
               (compojure/routes
                (GET (str "/" mine-ns) []
                  (partial with-init {:semantic-markup :home
                                      :mine mine}))
                (GET (str "/" mine-ns "/report/:class/:id") [id]
                  (partial with-init {:semantic-markup :report
                                      :mine mine
                                      :object-id id})))))

      (GET "*" []
        (partial with-init {})))

    (not-found not-found-page)))
