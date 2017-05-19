(ns bluegenes.routes
  (:require [compojure.core :refer [GET POST defroutes context ANY]]
            [compojure.route :refer [resources]]
             [hiccup.page :refer [include-js include-css html5]]
            [bluegenes.api.modelcount :refer [modelcount modelcount-children cache cacheall]]
            [bluegenes.index :as index]
            [ring.util.response :refer [response resource-response]]))

(defroutes routes
  (GET "/" []
       (index/index))

  (resources "/")

  (GET "/worker" [worker]
  (assoc
      (response
       (slurp "./resources/public/js/compiled/worker.js"))
       :status 200
       :headers {"content-type" "text/javascript"}))

  (GET "/version" [] (response {:version "0.1.0"}))

  (context "/api/model/count" [paths]
    (GET "/cache" [mine] (cache mine)
      (response {:loading (str "We're caching counts for " mine "! Well done.")}))
    (GET "/cacheall" [] (cacheall)
      (response {:loading "We're caching counts for all mines! Please wait."}))
    (GET "/children" [path mine]
         (response (modelcount-children path mine)))
    (POST "/" [paths mine]
      (response (modelcount paths mine)))
    (GET "/" [paths mine]
      (response (modelcount paths mine))
    ))
  )
