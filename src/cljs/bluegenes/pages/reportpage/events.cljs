(ns bluegenes.pages.reportpage.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :refer [document-title]]
            [clojure.string :as string]))

(reg-event-db
 :handle-report-summary
 [document-title]
 (fn [db [_ summary]]
   (-> db
       (assoc-in [:report :summary] summary)
       (assoc :fetching-report? false))))

(reg-event-fx
 :fetch-report
 (fn [{db :db} [_ mine type id]]
   (let [type-kw (keyword type)
         service (get-in db [:mines mine :service])
         q       {:from type
                  :select (-> db :assets :summary-fields mine type-kw)
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
              (assoc-in [:tools :entity] entity))
      :dispatch-n [[::tools/fetch-tools]
                   [:fetch-report (keyword mine) type id]
                   (when (= type "Gene")
                     [:fetch-fasta (keyword mine) id])]})))
