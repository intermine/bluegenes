(ns bluegenes.pages.templates.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [bluegenes.pages.templates.helpers :as template-helpers]))

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
   (get-in db [:assets :templates (:current-mine db)])))

(reg-sub
 :template-chooser/count
 (fn [db]
   (get-in db [:components :template-chooser :count])))

(reg-sub
 :template-chooser/results-preview
 (fn [db]
   (get-in db [:components :template-chooser :results-preview])))

(reg-sub
 :template-chooser/counting?
 (fn [db]
   (get-in db [:components :template-chooser :counting?])))

(reg-sub
 :template-chooser/fetching-preview?
 (fn [db]
   (get-in db [:components :template-chooser :fetching-preview?])))

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
         filter-fn
         (fn [[id details]]
           (if category
             (some? (some (fn [tag] (filter-pred tag category))
                          (:tags details)))
             true))]
     (sort-by
      (fn [[template-name template-details]]
        (let [rank (:rank template-details)]
          ;; Template ranks come back as strings, either "unranked", or
          ;; integers that have become stringy, e.g. "12". If we don't parse
          ;; them into ints, the order becomes 1, 11, 12, 2, 23, 25, 3, etc.
          ;; but we also need to handle the genuine strings, which become NaN
          ;; when we try to parse them.
          (if (.isNaN js/Number rank)
            ;; unranked == last please.
            ;; I sincerely hope we never have 100k templates
            99999
            ;; if it's a number, just return it.
            rank)))
      < (filter (partial template-contains-string? text-filter)
                (filter filter-fn templates))))))

(reg-sub
 :selected-template-name
 (fn [db _]
   (-> db :components :template-chooser :selected-template :name)))

(reg-sub
 :template-chooser/model
 (fn [db _]
   (:model (:assets db))))

(reg-sub
 :selected-template
 (fn [db _]
   (get-in db [:components :template-chooser :selected-template])))

(reg-sub
 :selected-template-category
 (fn [db _]
   (get-in db [:components :template-chooser :selected-template-category])))

(reg-sub
 :selected-template-service
 (fn [db _]
   (get-in db [:components :template-chooser :selected-template-service])))
