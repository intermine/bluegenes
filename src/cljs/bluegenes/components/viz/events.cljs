(ns bluegenes.components.viz.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.utils :refer [suitable-config?]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.viz.views :refer [all-viz]]))

(reg-event-fx
 :viz/run-queries-for-entity
 (fn [{db :db} [_ entity]]
   (let [model (get-in db [:mines (:current-mine db) :service :model :classes])]
     {:dispatch-n (map (fn [{:keys [config query key]}]
                         (when (suitable-config? model entity config)
                           [:viz/run-query key (query (:value entity))]))
                       all-viz)})))

(reg-event-fx
 :viz/run-query
 (fn [{db :db} [_ key query]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (fetch/records service query)
                :on-success [:viz/run-query-success key]}})))

(reg-event-db
 :viz/run-query-success
 (fn [db [_ key response]]
   (assoc-in db [:results :viz key] (:results response))))

(reg-event-db
 :viz/clear
 (fn [db [_]]
   (assoc-in db [:results :viz] nil)))
