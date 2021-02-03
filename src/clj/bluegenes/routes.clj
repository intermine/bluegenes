(ns bluegenes.routes
  (:require [compojure.core :as compojure :refer [GET defroutes context]]
            [compojure.route :refer [resources]]
            [ring.util.response :as response :refer [response]]
            [bluegenes.ws.auth :as auth]
            [bluegenes.ws.ids :as ids]
            [bluegenes.ws.rss :as rss]
            [bluegenes.ws.lookup :as lookup]
            [bluegenes.index :refer [index]]
            [config.core :refer [env]]
            [bluegenes.utils :refer [env->mines]]))

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

; Define the top level URL routes for the server
(def routes
  (compojure/let-routes [mines (env->mines env)]
    ;;serve compiled files, i.e. js, css, from the resources folder
    (resources "/")

    (GET "/version" [] (response {:version "0.1.0"}))

    ;; Anything within this context is the API web service.
    (context "/api" []
      (context "/auth" [] auth/routes)
      (context "/ids" [] ids/routes)
      (context "/rss" [] rss/routes))

    ;; Dynamic routes for handling permanent URL resolution on configured mines.
    (apply compojure/routes
           (for [{mine-ns :namespace :as mine} mines]
             (context (str "/" mine-ns) []
               (GET ["/:lookup" :lookup #"[^:/.]+:[^:/.]+"] [lookup]
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
      (partial with-init {}))))
