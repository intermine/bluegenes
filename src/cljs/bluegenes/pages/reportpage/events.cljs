(ns bluegenes.pages.reportpage.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :refer [document-title]]
            [clojure.string :as string]
            [bluegenes.pages.reportpage.utils :as utils]))

(reg-event-fx
 :handle-report-summary
 [document-title]
 (fn [{db :db} [_ summary]]
   {:db (-> db
            (assoc-in [:report :summary] summary)
            (assoc-in [:report :title] (utils/title-column summary))
            (assoc :fetching-report? false))
    :dispatch-n [[:viz/run-queries]
                 [::tools/load-tools]]}))

(reg-event-fx
 :fetch-report
 (fn [{db :db} [_ mine type id]]
   (let [service (get-in db [:mines mine :service])
         attribs (->> (get-in service [:model :classes (keyword type) :attributes])
                      (mapv (comp #(str type "." %) :name val)))
         q       {:from type
                  :select attribs
                  :where [{:path (str type ".id")
                           :op "="
                           :value id}]}]
     {:im-chan {:chan (fetch/rows service q {:format "json"})
                :on-success [:handle-report-summary]}})))

(reg-event-db
 :handle-fasta
 (fn [db [_ fasta]]
   (cond-> db
     ;; If there's no FASTA the API will return "Nothing was found for export".
     (string/starts-with? fasta ">")
     (assoc-in [:report :fasta] fasta))))

(reg-event-fx
 :fetch-fasta
 (fn [{db :db} [_ mine id]]
   (let [service (get-in db [:mines mine :service])
         q {:from "Gene"
            :select ["Gene.id"]
            :where [{:path "Gene.id"
                     :op "="
                     :value id}]}]
     {:im-chan {:chan (fetch/fasta service q)
                :on-success [:handle-fasta]}})))

(reg-event-fx
 :load-report
 (fn [{db :db} [_ mine type id]]
   (let [entity {:class type
                 :format "id"
                 :value id}]
     {:db (-> db
              (assoc :fetching-report? true)
              (dissoc :report)
              (assoc-in [:tools :entities (keyword type)] entity))
      :dispatch-n [[::tools/fetch-tools]
                   [:fetch-report (keyword mine) type id]
                   (when (= type "Gene")
                     [:fetch-fasta (keyword mine) id])]})))
