(ns redgenes.routes
  (:require [compojure.core :refer [GET POST defroutes context ANY]]
            [redgenes.api.modelcount :refer [modelcount cache]]
            [ring.util.response :refer [response]]))



(defroutes routes
  (GET "/version" [] (response {:version "0.1.0"}))
  (context "/model/count" [paths]
    (GET "/cache" [mine] (cache mine) (response {:loading "Loading initiated! Please wait."}))
    (POST "/" [paths mine]
      (response (modelcount paths mine)))))
