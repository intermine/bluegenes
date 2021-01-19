(ns bluegenes.ws.lookup
  (:require [imcljs.fetch :as im-fetch]
            [ring.util.http-response :as response]
            [config.core :refer [env]]
            [clojure.string :as str]
            [cheshire.core :as cheshire]))

(defn handle-failed-lookup [lookup-string error-string]
  (let [msg [:span "Failed to parse permanent URL for " [:em lookup-string] " "
             [:code error-string]]]
    (-> (response/found "/")
        (assoc :session {:init {:events [[:messages/add
                                          {:style "warning"
                                           :timeout 0
                                           :markup msg}]]}}))))

(defn ws [lookup-string]
  (try
    (let [[object-type identifier] (str/split lookup-string #":")
          object-type (str/capitalize object-type)
          service {:root (:bluegenes-default-service-root env)
                   :model {:name "genomic"}}
          q {:from object-type
             :select [(str object-type ".id")]
             :where [{:path object-type
                      :op "LOOKUP"
                      :value identifier}]}
          res (im-fetch/rows service q)]
      (if-let [object-id (get-in res [:results 0 0])]
        (response/found (str "/" (:bluegenes-default-namespace env)
                             "/" "report"
                             "/" object-type
                             "/" object-id))
        (handle-failed-lookup lookup-string "The identifier does not seem to be in the database anymore.")))
    (catch Exception e
      (let [{:keys [body]} (ex-data e)
            {:keys [error]} (cheshire/parse-string body true)]
        (handle-failed-lookup lookup-string (or (not-empty error)
                                                "Error occurred when querying identifier"))))))
