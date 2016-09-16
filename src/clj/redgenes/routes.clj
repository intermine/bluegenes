(ns redgenes.routes
  (:require [compojure.core :refer [GET POST defroutes context ANY]]
            [redgenes.api.modelcount :refer [modelcount]]
            [ring.util.response :refer [response]]))


(defroutes model-count
  (POST "/" [paths] (response (modelcount paths)))
  )

(defroutes routes
  (GET "/version" [] (response {:version "0.1.0"}))
  (context "/model/count" [] model-count))
