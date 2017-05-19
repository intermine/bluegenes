(ns bluegenes.sections.reportpage.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse select transform]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.search :as search]
            [imcljs.fetch :as fetch]
            [imcljsold.filters :as filters]
            [com.rpl.specter :as s]))

(reg-event-db
  :handle-report-summary
  (fn [db [_ summary]]
    (-> db
        (assoc-in [:report :summary] summary)
        (assoc :fetching-report? false))))

(reg-event-fx
  :fetch-report
  (fn [{db :db} [_ mine type id]]
    (let [type-kw (keyword type)
          q {:from type
             :select (-> db :assets :summary-fields mine type-kw)
             :where [{:path (str type ".id")
                      :op "="
                      :value id}]}]

      {:im-chan {:chan (fetch/rows (get-in db [:mines mine :service]) q {:format "json"})
                 :on-success [:handle-report-summary]}})))


(reg-event-db
  :filter-report-collections
  (fn [db [_ mine type oid]]
    (let [summary-fields (-> db :assets :summary-fields mine)
          type-key (keyword type)
          collections (-> db :mines mine :service :model :classes type-key :collections)]
      (assoc-in db [:report :collections]
                (map (fn [[_ {:keys [name referencedType]}]]
                       (let [summary-paths (-> referencedType keyword summary-fields)]
                         {:class referencedType
                          :service (get-in db [:mines mine :service])
                          :query {:from type
                                  :select (map (fn [path]
                                                 (str name "."
                                                      (clojure.string/join "."
                                                                           (drop 1 (clojure.string/split path ".")))))
                                               summary-paths)
                                  :where [{:op "="
                                           :path (str type ".id")
                                           :value oid}]}})) collections)))))

(reg-event-fx
  :filter-report-templates
  (fn [{db :db} [_ mine type id]]
    (let [model (-> db :mines mine :service :model :classes)
          templates (-> db :assets :templates mine)]
      {:db (assoc-in db [:report :templates]
                     (into {} (traverse
                                [s/ALL
                                 (s/selected?
                                   s/LAST
                                   :where #(= 1 (count (filter (fn [c] (:editable c)) %)))
                                   s/ALL
                                   :path #(= type (filters/end-class model %)))] templates)))
       :dispatch [:filter-report-collections mine type id]})))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ mine type id]]
    {:db (-> db
             (assoc :fetching-report? true)
             (dissoc :report))
     :dispatch-n [[:fetch-report (keyword mine) type id]
                  [:filter-report-templates (keyword mine) type id]]}))
