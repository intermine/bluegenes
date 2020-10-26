(ns bluegenes.pages.reportpage.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :refer [document-title]]
            [clojure.string :as string]
            [bluegenes.pages.reportpage.utils :as utils]
            [bluegenes.route :as route]))

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

(defn class-has-dataset? [model-classes class-kw]
  (contains? (get-in model-classes [class-kw :collections])
             :dataSets))

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
                     [:fetch-fasta (keyword mine) id])
                   [::fetch-lists (keyword mine) id]
                   (when (class-has-dataset? (get-in db [:mines (keyword mine) :service :model :classes])
                                             (keyword type))
                     [::fetch-sources (keyword mine) type id])]})))

(reg-event-fx
 ::fetch-lists
 (fn [{db :db} [_ mine-kw id]]
   (let [service (get-in db [:mines mine-kw :service])]
     {:im-chan {:chan (fetch/lists-containing service {:id id})
                :on-success [::handle-lists]}})))

(reg-event-db
 ::handle-lists
 (fn [db [_ lists]]
   (assoc-in db [:report :lists] lists)))

(reg-event-fx
 ::fetch-sources
 (fn [{db :db} [_ mine-kw class id]]
   (let [service (get-in db [:mines mine-kw :service])
         q {:from class
            :select ["dataSets.description"
                     "dataSets.url"
                     "dataSets.name"
                     "dataSets.id"]
            :where [{:path (str class ".id")
                     :op "="
                     :value id}]}]
     {:im-chan {:chan (fetch/rows service q {:format "json"})
                :on-success [::handle-sources]}})))

(defn views->results
  "Convert a `fetch/rows` response map into a vector of maps from keys
  corresponding to the tail of the view, and values being the pair of the key.
  Ex. [{:name 'BioGRID interaction data set' :url 'http://www.thebiogrid.org/downloads.php'}
       {:name 'Panther orthologue and paralogue predictions' :url 'http://pantherdb.org/'}]"
  [{:keys [views results] :as _res}]
  (let [concise-views (map #(-> % (string/split #"\.") last keyword)
                           views)]
    (mapv (partial zipmap concise-views)
          results)))

(reg-event-db
 ::handle-sources
 (fn [db [_ res]]
   (assoc-in db [:report :sources] (views->results res))))

(reg-event-fx
 ::open-in-region-search
 (fn [{db :db} [_ chromosome-location]]
   {:dispatch-n [[::route/navigate ::route/regions]
                 [:regions/set-to-search chromosome-location]
                 [:regions/run-query]]}))
