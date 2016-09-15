(ns redgenes.routes
  (:require [compojure.core :refer [GET POST defroutes context ANY]]
            [ring.util.response :refer [response]]))


(defroutes model-count
  (POST "/" [paths] (println paths) (response {:Genes 14 :Genes.proteins 2}))
  )

(defroutes routes
  (GET "/version" [] (response {:version "0.1.0"}))
  (context "/model/count" [] model-count))
