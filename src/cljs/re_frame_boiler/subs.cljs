(ns re-frame-boiler.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

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
  :suggestion-results
  (fn [db _]
    (:suggestion-results db)))

(reg-sub
  :search-term
  (fn [db _]
    (:search-term db)))

(reg-sub
  :templates
  (fn [db _]
    (:templates (:assets db))))

(reg-sub
  :lists
  (fn [db _]
    (:lists (:assets db))))

(reg-sub
  :summary-fields
  (fn [db _]
    (:summary-fields (:assets db))))

(reg-sub
  :progress-bar-percent
  (fn [db _]
    (:progress-bar-percent db)))

(reg-sub
  :selected-template
  (fn [db _]
    (let [template (:selected-template db)]
      (-> db :assets :templates template))))