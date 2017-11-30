(ns bluegenes.ws.ids
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.util.http-response :as response]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [lower-case]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

(defn parse-identifiers
  [s]
  (let [matcher (re-matcher (re-pattern "[^(\\s|,;)\"']+|\"([^\"]*)\"|'([^']*)'") s)]
    (->> matcher
         (partial re-find)
         repeatedly
         (take-while some?)
         (map (partial (comp last (partial take-while some?)))))))

(defn parse-file
  [[file-name {:keys [filename content-type tempfile size]}]]
  (parse-identifiers (slurp tempfile)))

(def multipart-options ["caseSensitive"])

(defn parse-request-for-ids [{:keys [body multipart-params] :as req}]
  (let [; Remove the multipart form fields that are options
        files          (apply dissoc multipart-params multipart-options)
        ; Build a map of the multipart form fields that are options
        options        (select-keys multipart-params multipart-options)
        ; Should the parsing be case sensitive?
        case-sensitive (= "true" (get options "caseSensitive"))]
    ; Parse the identifiers and remove duplicates (convert to lower case if case-insensitive)
    (let [total (distinct (map (if case-sensitive lower-case identity) (mapcat parse-file files)))]
      ; Return the parsed identifiers and the total count
      {:identifiers total
       :total (count total)})))

(defroutes routes (wrap-multipart-params (POST "/parse" req (response/ok (parse-request-for-ids req)))))

