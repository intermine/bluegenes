(ns bluegenes.components.search.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [oops.core :refer [ocall oget oget+]]))

(reg-sub
 :search-term
 (fn [db _]
   (:search-term db)))

(reg-sub
 :quicksearch-selected-index
 (fn [db _]
   (:quicksearch-selected-index db)))

(reg-sub
 :suggestion-results
 (fn [db _]
   (:suggestion-results db)))

(reg-sub
 :search/full-results
 (fn [db]
   (:search-results db)))

(reg-sub
 :search/active-filter
 (fn [db _]
   (:active-filter (:search-results db))))

(reg-sub
 :search/highlight?
 (fn [db _]
   (:highlight-results (:search-results db))))

(reg-sub
 :search/loading?
 (fn [db _]
   (:loading? (:search-results db))))

(reg-sub
 :search/am-i-selected?
 (fn [db [_ result]]
   (let [selected-results (get-in db [:search :selected-results])]
     (contains? selected-results result))))

(reg-sub
 :search/all-selected
 (fn [db _]
   (get-in db [:search :selected-results])))


 (reg-sub
  :search/some-selected?
  (fn [db _]
      (> (count (get-in db [:search :selected-results])) 0)))
