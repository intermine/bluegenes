(ns bluegenes.components.templates.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [bluegenes.components.templates.helpers :as template-helpers]
            [imcljsold.filters :as filters]))

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
    (let [template-packages (reduce (fn [total [mine-kw templates]]
                                      (doall (reduce (fn [t [name query]]
                                                       (assoc t (keyword mine-kw name) query)) total templates)))
                                    {} (seq (get-in db [:assets :templates])))]
      template-packages)))

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
  :template-chooser/model
  (fn [db _]
    (:model (:assets db))))

(reg-sub
  :selected-template
  (fn [db _]
    (get-in db [:components :template-chooser :selected-template])))

(def ns->kw (comp keyword (fnil namespace :nil)))
(def name->kw (comp keyword name))
(def ns->vec (juxt ns->kw name->kw))

(reg-sub
  :template-chooser/selected-template-constraints
  (fn [db]
    (let [source      (ns->kw (get-in db [:components :template-chooser :selected-template-name]))
          classes       (get-in db [:mines source :service :model :classes])
          constraints (get-in db [:components :template-chooser :selected-template :where])]
      (if (and source classes constraints)
        (map (fn [constraint]
               (assoc constraint
                 :field-type (filters/path-type classes (:path constraint)
                                                (first (filter (fn [x] (contains? x :type)) constraints))))) constraints)
        nil))))

(reg-sub
  :selected-template-category
  (fn [db _]
    (get-in db [:components :template-chooser :selected-template-category])))


(reg-sub
  :selected-template-service
  (fn [db _]
    (get-in db [:components :template-chooser :selected-template-service])))