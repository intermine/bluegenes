(ns bluegenes.ws.lookup
  (:require [imcljs.fetch :as im-fetch]
            [ring.util.response :refer [content-type response]]
            [ring.util.http-response :as response]
            [clojure.string :as str]
            [cheshire.core :as cheshire]
            [clj-http.client :refer [with-middleware]]
            [bluegenes.utils :as utils]
            [config.core :refer [env]]))

(def extension->content-type
  {"rdf" "application/rdf+xml;charset=UTF-8"})

(defn handle-failed-lookup [lookup-string {:keys [namespace]} error-string]
  (let [msg [:span "Failed to parse permanent URL for " [:em lookup-string] " "
             [:code error-string]]]
    (-> (response/found (str (:bluegenes-deploy-path env) "/" namespace))
        (assoc :session {:init {:events [[:messages/add
                                          {:style "warning"
                                           :timeout 0
                                           :markup msg}]]}}))))

(defn query-identifier [lookup-string [object-type identifier] {:keys [root namespace] :as mine}]
  (let [object-type (str/capitalize object-type)
        service {:root root
                 :model {:name "genomic"}}
        q {:from object-type
           :select [(str object-type ".id")]
           :where [{:path object-type
                    :op "LOOKUP"
                    :value identifier}]}
        res (im-fetch/rows service q)]
    (if-let [object-id (get-in res [:results 0 0])]
      (response/found (str (:bluegenes-deploy-path env)
                           "/" namespace
                           "/" "report"
                           "/" object-type
                           "/" object-id))
      (handle-failed-lookup lookup-string mine "The identifier does not seem to be in the database anymore."))))

(defn ws [lookup-string {:keys [root] :as mine}]
  (try
    (let [[object-type identifier extension :as lookupv] (str/split lookup-string #"[:.]")
          service {:root root
                   :model {:name "genomic"}}]
      (if (not-empty extension)
        ;; Specify middleware so we can accept other formats than JSON.
        (with-middleware [#'clj-http.client/wrap-request
                          #'utils/wrap-accept-all]
          (let [out (im-fetch/entity-representation service (str object-type ":" identifier) extension)]
            (-> (response out)
                (content-type (extension->content-type extension "text/plain;charset=UTF-8")))))
        (query-identifier lookup-string lookupv mine)))
    (catch Exception e
      (let [{:keys [body]} (ex-data e)
            {:keys [error]} (cheshire/parse-string body true)]
        (handle-failed-lookup lookup-string mine (or (not-empty error)
                                                     "Error occurred when parsing identifier"))))))
