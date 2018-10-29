(ns bluegenes.ws.ids
  (:require [compojure.core :refer [GET POST defroutes]]
            [ring.util.http-response :as response]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [clojure.string :refer [lower-case]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]))

;;;;;;;;;;;;;;;;;;;;
;;;; These services bulk-parse IDs on the server side, for the upload / idresolver
;;;; SEE bluegenes/src/cljs/bluegenes/components/idresolver/README.md for details
;;;;;;;;;;;;;;;;;;;;

(defn parse-identifiers
  "Given a string, use regular expressions to parse values into a list"
  [s]
  (when (some? s)
    (let [matcher (re-matcher (re-pattern "[^(\\s|,;)\"']+|\"([^\"]*)\"|'([^']*)'") s)]
      ; Uber threading!
      (->> matcher
           ; Build a re-find function using the matcher
           (partial re-find)
           ; Repeatedly call it forever
           repeatedly
           ; That is until we no longer have a value
           (take-while some?)
           ; There's a lot going on here:
           ; Re-find returns nested lists of results, so we collect the last non-nil value from each
           (map (partial (comp last (partial take-while some?))))))))

(defn parse-file
  "Parse identifiers from a file on disk"
  [[file-name {:keys [filename content-type tempfile size]}]]
  (parse-identifiers (slurp tempfile)))

; Keys from a multipart request that indicate anything other than a reference to a file (see below)
(def multipart-options ["caseSensitive" "text"])

(defn parse-request-for-ids
  "Multipart params come in as key -> value pairs. We assume that each k/v is a reference to a file [filename filedata]
  unless the keys are words that indicate options, specifically text and caseSensitive"
  [{:keys [multipart-params] :as req}]
  ; An example request that contains files, plaintext, and options:
  (comment {:multipart-params [["file1.txt" {:filename "file1.txt" :content-type "text/plain" :tempfile "d80sd08d"}]
                               ["file2.txt" {:filename "file2.txt" :content-type "text/plain" :tempfile "3hjdgou3"}]
                               ["text" "Eve thor zen fkh batman"]
                               ["caseSensitive" "true"]]})
  (let [; Remove the multipart form fields that are "option" flags
        files (apply dissoc multipart-params multipart-options)
        ; Any text to be parsed from a string should be passed as the "text" multipart parameter
        text (get multipart-params "text")
        ; Build a map of the multipart form fields that are options
        options (select-keys multipart-params multipart-options)
        ; Should the parsing be case sensitive?
        case-sensitive (= "true" (get options "caseSensitive"))]
    ; Parse the identifiers and remove duplicates (convert to lower case if case-insensitive)
    (let [total (distinct (map (if case-sensitive lower-case identity) (concat (mapcat parse-file files) (parse-identifiers text))))]
      ; Return the parsed identifiers and the total count
      {:identifiers total
       :total (count total)})))

(defroutes routes (wrap-multipart-params (POST "/parse" req (response/ok (parse-request-for-ids req)))))
