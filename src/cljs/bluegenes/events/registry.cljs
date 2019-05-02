(ns bluegenes.events.registry
  (:require [re-frame.core :refer [reg-event-db reg-event-fx subscribe]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]))

(reg-event-fx
 ;; these are the intermines we'll allow users to switch to
 ::load-other-mines
 (fn [{db :db}]
   {:im-chan
    {:chan (fetch/registry false)
     :on-success [::success-fetch-registry]}}))

(reg-event-db
 ::success-fetch-registry
 (fn [db [_ mines]]
   (let [registry-mines-response (js->clj mines :keywordize-keys true)
         ;; extricate the mines from the deeply nested response object
         registry-mines (get-in registry-mines-response [:body :instances])
         ;;they *were* in an array, but a map would be easier to reference mines
         registry-mines-map
         (reduce
          (fn [new-map mine]
            (assoc new-map (keyword (:namespace mine)) mine))
          {} registry-mines)]
     (assoc-in db [:registry] registry-mines-map))))
