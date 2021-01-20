(ns bluegenes.ws.lookup
  (:require [imcljs.fetch :as im-fetch]
            [ring.util.http-response :as response]
            [clojure.string :as str]
            [cheshire.core :as cheshire]))

(defn handle-failed-lookup [lookup-string {:keys [namespace]} error-string]
  (let [msg [:span "Failed to parse permanent URL for " [:em lookup-string] " "
             [:code error-string]]]
    (-> (response/found (str "/" namespace))
        (assoc :session {:init {:events [[:messages/add
                                          {:style "warning"
                                           :timeout 0
                                           :markup msg}]]}}))))

(defn ws [lookup-string {:keys [root namespace] :as mine}]
  (try
    (let [[object-type identifier] (str/split lookup-string #":")
          object-type (str/capitalize object-type)
          service {:root root
                   :model {:name "genomic"}}
          q {:from object-type
             :select [(str object-type ".id")]
             :where [{:path object-type
                      :op "LOOKUP"
                      :value identifier}]}
          res (im-fetch/rows service q)]
      (if-let [object-id (get-in res [:results 0 0])]
        (response/found (str "/" namespace
                             "/" "report"
                             "/" object-type
                             "/" object-id))
        (handle-failed-lookup lookup-string mine "The identifier does not seem to be in the database anymore.")))
    (catch Exception e
      (let [{:keys [body]} (ex-data e)
            {:keys [error]} (cheshire/parse-string body true)]
        (handle-failed-lookup lookup-string mine (or (not-empty error)
                                                     "Error occurred when querying identifier"))))))
