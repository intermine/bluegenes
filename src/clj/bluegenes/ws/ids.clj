(ns bluegenes.ws.ids
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.util.http-response :as response]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(defn parse-identifiers
  [s]
  (let [matcher (re-matcher (re-pattern "[^(\\s|,;)\"']+|\"([^\"]*)\"|'([^']*)'") s)]
    (->> matcher
         (partial re-find)
         repeatedly
         (take-while some?)
         (map (partial (comp last (partial take-while some?)))))))

(defroutes routes
           ; Expects a POST raw body of type text/plain identifiers
           (wrap-multipart-params (POST "/parse" {body :body :as req}
                                    (response/ok (let [parsed (parse-identifiers (slurp body))]
                                                   {:identifiers parsed
                                                    :total (count parsed)})))))

