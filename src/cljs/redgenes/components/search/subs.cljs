(ns redgenes.components.search.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

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
