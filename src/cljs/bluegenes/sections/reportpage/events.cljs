(ns bluegenes.sections.reportpage.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(defn build-query [model summary-fields type id attribute]
  {:from type
   :select (get summary-fields type)
   :where [{:path (str type ".id")
            :op "="
            :value id}]})

(defn build-table-configuration []
  {:service {:root "beta.humanmine.org/beta"}
   :query {:from "Gene"
           :select ["symbol"
                    "secondaryIdentifier"
                    "dataSets.description"
                    "primaryIdentifier"
                    "organism.name"
                    "dataSets.name"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "BananaMine"}
                      :url (fn [vocab] (str "#/reportpage/"
                                            (:mine vocab) "/"
                                            (:class vocab) "/"
                                            (:objectId vocab)))}}})

(defn build-configuration-map [model summary-fields type id attribute]
  (-> (build-table-configuration)
      (assoc :query (build-query model summary-fields type id attribute))))

