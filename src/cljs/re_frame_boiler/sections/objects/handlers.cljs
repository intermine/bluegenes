(ns re-frame-boiler.sections.objects.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]))


(reg-fx
  :fetch-report
  (fn [id]
    (let [q {:from "Gene"
             :select "symbol"
             :where {:symbol "eve"}}]
      #_(go (println "fetching q" (<! (search/raw-query-rows {:root "www.flymine.org/query"}
                                                        q
                                                        {:format "json"})))))))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ id]]
    {:db (assoc db :fetching-report? true)
     :fetch-report id}))