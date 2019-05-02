(ns bluegenes.events.registry
  (:require [re-frame.core :refer [reg-event-db reg-event-fx subscribe]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]))

;; this is not crazy to hardcode. The consequences of a mine that is lower than
;; the minimum version using bluegenes could potentially result in corrupt lists
;; so it *should* be hard to change.
;;https://github.com/intermine/intermine/issues/1482
(def min-intermine-version 27)

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
            ;;only store a mine entry if the API version is high enough.
            (if (>= (.parseInt js/window (:api_version mine) 10) min-intermine-version)
              (assoc new-map (keyword (:namespace mine)) mine)
              new-map))
          {} registry-mines)]
     (assoc-in db [:registry] registry-mines-map))))
