(ns re-frame-boiler.components.templates.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [re-frame-boiler.components.templates.helpers :as template-helpers]))

(defn template-contains-string?
  "Return true if a template's description contains a string"
  [string [_ details]]
  (if string
    (if-let [description (:description details)]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(reg-sub
  :templates
  (fn [db _]
    (:templates (:assets db))))

(reg-sub
  :template-chooser/count
  (fn [db]
    (get-in db [:components :template-chooser :count])))

(reg-sub
  :template-chooser/counting?
  (fn [db]
    (get-in db [:components :template-chooser :counting?])))

(reg-sub
  :template-chooser/text-filter
  (fn [db]
    (get-in db [:components :template-chooser :text-filter])))

(reg-sub
  :template-chooser-categories
  :<- [:templates]
  (fn [templates]
    (template-helpers/categories templates)))

(reg-sub
  :templates-by-category
  :<- [:templates]
  :<- [:selected-template-category]
  :<- [:template-chooser/text-filter]
  (fn [[templates category text-filter]]
    (let [filter-pred (fn [tag category] (= tag (str "im:aspect:" category)))
          filter-fn   (fn [[id details]]
                        (if category
                          (some? (some (fn [tag] (filter-pred tag category)) (:tags details)))
                          true))]
      (filter (partial template-contains-string? text-filter) (filter filter-fn templates)))))

(reg-sub
  :selected-template-name
  (fn [db _]
    (-> db :components :template-chooser :selected-template :name)))

(reg-sub
  :selected-template
  (fn [db _]
    (get-in db [:components :template-chooser :selected-template])))

(reg-sub
  :selected-template-category
  (fn [db _]
    (get-in db [:components :template-chooser :selected-template-category])))