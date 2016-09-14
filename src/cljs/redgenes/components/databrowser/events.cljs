(ns redgenes.components.databrowser.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.counts :as counts]
            [accountant.core :refer [navigate!]]))

(defn count-multiple-rows
  "Given a set of items and a root, calculate paths and get counts for all of them."
  [{root :root token :token} path]
  (go (<! (counts/count-rows {:root @(subscribe [:mine-url])} "Gene.id")))
  )

(reg-event-fx
  :databrowser/fetch-all-counts
  (fn [{db :db}]
    {:db           (assoc db :fetching-counts? true)
     :databrowser/fetch-counts {:connection
        {:root @(subscribe [:mine-url])}
         :paths ["Gene.proteins" "Gene.dataSets"]} }))


(reg-fx
  :databrowser/fetch-counts
  (fn [{connection :connection paths :paths}]
    (doall (map (fn [path]
      (go (let [res (<! (counts/count-rows connection (str path ".id")))]
            ;     (re-frame/dispatch [:save-count res])
            (.log js/console "%cres" "color:hotpink;font-weight:bold;" (clj->js res))
      ))
    ) ["Gene" "Gene.dataSets" "Gene.proteins" "Gene.interactions"]))
    ))
