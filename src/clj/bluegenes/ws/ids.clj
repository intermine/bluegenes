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

(defn parse-file [[file-name {:keys [filename content-type tempfile size]}]]
  (parse-identifiers (slurp tempfile)))

(defn parse-request-for-ids [{:keys [body multipart-params] :as req}]
  (let [
        ; Parse identifiers from file upload
        from-upload (mapcat parse-file multipart-params)
        ; Parsed identifiers from the raw body
        from-body   (parse-identifiers (slurp body))]
    ; Concat the results together and return unique values
    (let [total (distinct (concat from-upload from-body))]
      {:identifiers total
       :total (count total)})))

(defroutes routes (wrap-multipart-params (POST "/parse" req (response/ok (parse-request-for-ids req)))))

