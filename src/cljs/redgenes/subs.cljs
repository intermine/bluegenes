(ns redgenes.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            [redgenes.components.databrowser.subs]
            [redgenes.mines :as mines]
            [redgenes.components.search.subs]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

(reg-sub
  :short-name
  (fn [db]
    (:short-name db)))


(defn merge-mines [db]
  (merge mines/mines (:temporary-mine db)))

(reg-sub
  :mine-url
  (fn [db]
    (let [mine-name (:mine-name db)
          url       (:url (:mine (mine-name (merge-mines db))))]
      (str "http://" url))))

(reg-sub :mine-default-organism
         (fn [db]
           (let [mine-name (:mine-name db)
                 organism  (:abbrev (mine-name (merge-mines db)))]
             organism)))

(reg-sub :mines
         (fn [db]
           (merge-mines db)))

(reg-sub
  :mine-name
  (fn [db]
    (:mine-name db)))

(reg-sub
  :active-panel
  (fn [db _]
    (:active-panel db)))

(reg-sub
  :panel-params
  (fn [db _]
    (:panel-params db)))

(reg-sub
  :app-db
  (fn [db _] db))

(reg-sub
  :who-am-i
  (fn [db _]
    (:who-am-i db)))

(reg-sub
  :fetching-report?
  (fn [db _]
    (:fetching-report? db)))

(reg-sub
  :runnable-templates
  (fn [db _]
    (:templates (:report db))))

(reg-sub
  :collections
  (fn [db _]
    (:collections (:report db))))

(reg-sub
  :model
  (fn [db _]
    (:model (:assets db))))

(reg-sub
  :mine-icons
  (fn [db [_]]
    (reduce (fn [total [mine-kw {icon :icon}]]
              (assoc total mine-kw icon)) {} (get-in db [:mines]))))
(reg-sub
  :lists
  (fn [db [_]]
    (get-in db [:assets :lists])))

(reg-sub
  :summary-fields
  (fn [db _]
    (:summary-fields (:assets db))))

(reg-sub
  :report
  (fn [db _]
    (:report db)))

(reg-sub
  :progress-bar-percent
  (fn [db _]
    (:progress-bar-percent db)))

(reg-sub
  :saved-data
  (fn [db _]
    (:items (:saved-data db))))

(reg-sub
  :tooltip
  (fn [db]
    (get-in db [:tooltip :saved-data])))

(reg-sub
  :cache/organisms
  (fn [db]
    (get-in db [:cache :organisms])))
