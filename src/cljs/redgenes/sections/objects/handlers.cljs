(ns redgenes.sections.objects.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse select transform]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.filters :as filters]
            [com.rpl.specter :as s]))

(reg-event-db
  :handle-report-summary
  (fn [db [_ summary]]
    (-> db
        (assoc-in [:report :summary] summary)
        (assoc :fetching-report? false))))

(reg-fx
  :fetch-report
  (fn [[db type id]]
    (let [type-kw (keyword type)
          q       {:from   type
                   :select (-> db :assets :summary-fields type-kw)
                   :where  {:id id}}]
      (go (dispatch [:handle-report-summary (<! (search/raw-query-rows
                                                  {:root "www.flymine.org/query"}
                                                  q
                                                  {:format "json"}))])))))


(reg-event-db
  :filter-report-collections
  (fn [db [_ type oid]]
    (let [model          (-> db :assets :model)
          templates      (-> db :assets :templates)
          summary-fields (-> db :assets :summary-fields)
          type-key       (keyword type)
          collections    (-> db :assets :model type-key :collections)]
      (assoc-in db [:report :collections] (map (fn [[_ {:keys [name referencedType]}]]
                                                 (let [summary-paths (-> referencedType keyword summary-fields)]
                                                   ; Create a query for each collection
                                                   {:class referencedType
                                                    :query {:from   type
                                                            :select (map (fn [path]
                                                                           (str name "."
                                                                                (clojure.string/join "."
                                                                                                     (drop 1 (clojure.string/split path ".")))))
                                                                         summary-paths)
                                                            :where  {:id oid}}})) collections)))))


(reg-event-fx
  :filter-report-templates
  (fn [{db :db} [_ type id]]
    (let [model     (-> db :assets :model)
          templates (-> db :assets :templates)]
      {:db       (assoc-in db [:report :templates]
                           (into {} (traverse
                                      [s/ALL
                                       (s/selected?
                                         s/LAST
                                         :where #(= 1 (count (filter (fn [c] (:editable c)) %)))
                                         s/ALL
                                         :path #(= type (filters/end-class model %)))] templates)))
       :dispatch [:filter-report-collections type id]})))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ type id]]
    {:db           (-> db
                       (assoc :fetching-report? true)
                       (dissoc :report))
     :fetch-report [db type id]
     :dispatch     [:filter-report-templates type id]}))



