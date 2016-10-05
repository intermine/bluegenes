(ns redgenes.components.databrowser.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.counts :as counts]
            [accountant.core :refer [navigate!]]))


(reg-event-fx
  :databrowser/fetch-all-counts
  (fn [{db :db}]
    {:db           (assoc db :fetching-counts? true)
     :databrowser/fetch-counts {
      :connection
        {:root @(subscribe [:mine-url])}
      :path "top"
      } }))


(reg-fx
  :databrowser/fetch-counts
  (fn [{connection :connection path :path}]
      (go (let [res (<! (counts/count-rows connection path))]
            (re-frame/dispatch [:databrowser/save-counts :human res])
      ))
    ))

(reg-event-db
 :databrowser/save-counts
 (fn [db [_ mine-name counts]]
   (assoc-in db [:databrowser/model-counts mine-name] counts)
))
