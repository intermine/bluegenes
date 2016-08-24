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
  :fetching-report?
  (fn [db _]
    (:fetching-report? db)))

(reg-sub
  :templates
  (fn [db _]
    (:templates (:assets db))))

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
  :lists
  (fn [db _]
    (:lists (:assets db))))

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