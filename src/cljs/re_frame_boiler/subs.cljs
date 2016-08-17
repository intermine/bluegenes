(ns re-frame-boiler.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [re-frame-boiler.components.templates.helpers :as template-helpers]))

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
  :template-chooser-categories
  :<- [:templates]
  (fn [templates]
    (template-helpers/categories templates)))

(reg-sub
  :templates-by-category
  :<- [:templates]
  :<- [:selected-template-category]
  (fn [[templates category]]
    (let [filter-pred (fn [tag category] (= tag (str "im:aspect:" category)))
          filter-fn   (if category
                        (fn [[id details]]
                          (some? (some (fn [tag]
                                         (filter-pred tag category)) (:tags details))))
                        identity)]
      (filter filter-fn templates))))

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

(reg-sub
  :selected-template-name
  (fn [db _]
    (:selected-template (:template-chooser (:components db)))))

(reg-sub
  :selected-template
  (fn [db _]
    (if-let [template (:selected-template (:template-chooser (:components db)))]
      (-> db :assets :templates template))))

(reg-sub
  :selected-template-category
  (fn [db _]
    (:selected-template-category (:template-chooser (:components db)))))