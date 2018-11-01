(ns bluegenes.pages.reportpage.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [imcljs.path :as path]))

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
   {:db (-> db
            (assoc :fetching-report? true)
            (dissoc :report))
    :dispatch [:fetch-report (keyword mine) type id]}))