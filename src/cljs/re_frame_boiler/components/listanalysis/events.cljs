(ns re-frame-boiler.components.listanalysis.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [imcljs.search :as search]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(reg-event-db
  :listanalysis/handle-results
  (fn [db [_ results]]
    (assoc-in db [:list-analysis :results] results)))

(reg-fx
  :listanalysis/get-enrichment
  (fn [values]
    (go (dispatch [:listanalysis/handle-results (<! values)]))))

(reg-event-fx
  :listanalysis/run
  (fn [{db :db} more]
    (let [enrichment-chan
          (search/enrichment
            {:root "www.flymine.org/query"}
            {:ids        (get-in db [:idresolver :saved (:temp (:panel-params db))])
             :maxp       0.05
             :widget     "pathway_enrichment"
             :correction "Holm-Bonferroni"})]
      {:db db
       :listanalysis/get-enrichment enrichment-chan})))