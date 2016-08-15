(ns re-frame-boiler.sections.objects.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]))

(reg-event
  :handle-report-summary
  (fn [db [_ summary]]
    (assoc-in db [:report :summary] summary)))

(reg-fx
  :fetch-report
  (fn [[db type id]]
    (let [type-kw (keyword type)
          q {:from   type
             :select (-> db :assets :summary-fields type-kw)
             :where  {:id id}}]
      (go (dispatch [:handle-report-summary (<! (search/raw-query-rows
                                                  {:root "www.flymine.org/query"}
                                                  q
                                                  {:format "json"}))])))))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ type id]]
    {:db           (-> db
                       (assoc :fetching-report? true)
                       (dissoc :report))
     :fetch-report [db type id]}))