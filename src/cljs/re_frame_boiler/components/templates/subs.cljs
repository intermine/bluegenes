(ns re-frame-boiler.components.templates.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [re-frame-boiler.components.templates.helpers :as template-helpers]))

(reg-sub
  :templates
  (fn [db _]
    (:templates (:assets db))))

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