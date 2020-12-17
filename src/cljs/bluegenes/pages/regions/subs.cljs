(ns bluegenes.pages.regions.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            [imcljs.entity :as entity]))

(reg-sub
 :regions/sequence-feature-types
 (fn [db]
   (let [model (get-in db [:mines (get db :current-mine) :service :model])]
     (entity/extended-by-tree model :SequenceFeature))))

(reg-sub
 :regions/sequence-feature-type-all-selected?
 (fn [db]
   (let [features  (get-in db [:regions :settings :feature-types])
         selected-features  (remove #(not (second %)) features)]
     (= (count features) (count selected-features)))))

(reg-sub
 :regions/error
 (fn [db]
   (get-in db [:regions :error])))

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
 :regions/loading
 (fn [db]
   (get-in db [:regions :loading])))

(reg-sub
 :regions/to-search
 (fn [db]
   (get-in db [:regions :to-search])))

(reg-sub
 :regions/example-search
 :<- [:current-mine]
 (fn [mine]
   (:regionsearch-example mine)))
