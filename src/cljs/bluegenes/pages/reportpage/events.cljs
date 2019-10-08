(ns bluegenes.pages.reportpage.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :refer [document-title]]))

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
         q       {:from type
                  :select (-> db :assets :summary-fields mine type-kw)
                  :where [{:path (str type ".id")
                           :op "="
                           :value id}]}]

     {:im-chan {:chan (fetch/rows (get-in db [:mines mine :service]) q {:format "json"})
                :on-success [:handle-report-summary]}})))

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
                   [:fetch-report (keyword mine) type id]]})))
