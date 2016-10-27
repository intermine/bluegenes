(ns redgenes.sections.regions.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            [imcljs.model :as m]))


(reg-sub
  :regions/sequence-feature-types
  (fn [db]
    (let [model (get-in db [:assets :model])]
      (m/descendant-classes-as-tree model :SequenceFeature))))

(reg-sub
  :regions/settings
  (fn [db]
    (get-in db [:regions :settings])))

(reg-sub
  :regions/regions-searched
  (fn [db]
    (get-in db [:regions :regions-searched])))

(reg-sub
  :regions/results
  (fn [db]
    (get-in db [:regions :results])))

(reg-sub
  :regions/to-search
  (fn [db]
    (get-in db [:regions :to-search])))