(ns bluegenes.components.search.subs
  (:require [re-frame.core :refer [reg-sub]]))

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
 :suggestion-error
 (fn [db _]
   (:suggestion-error db)))

(reg-sub
 :search/full-results
 (fn [db]
   (:search-results db)))

(reg-sub
 :search/results
 :<- [:search/full-results]
 (fn [full-results]
   (:results full-results)))

(reg-sub
 :search/facets-results
 :<- [:search/full-results]
 (fn [full-results]
   (:facets full-results)))

(reg-sub
 :search/active-filters
 :<- [:search/full-results]
 (fn [full-results]
   (:active-filters full-results)))

(reg-sub
 :search/active-filter?
 :<- [:search/active-filters]
 (fn [filters]
   (some? (seq filters))))

(reg-sub
 :search/category-filter?
 :<- [:search/active-filters]
 (fn [filters]
   (some? (:Category filters))))

(reg-sub
 :search/loading?
 :<- [:search/full-results]
 (fn [full-results]
   (:loading? full-results)))

(reg-sub
 :search/loading-remaining?
 :<- [:search/full-results]
 (fn [full-results]
   (:loading-remaining? full-results)))

(reg-sub
 :search/error
 :<- [:search/full-results]
 (fn [full-results]
   (:error full-results)))

(reg-sub
 :search/keyword
 :<- [:search/full-results]
 (fn [full-results]
   (:keyword full-results)))

(reg-sub
 :search/all-selected
 (fn [db _]
   (get-in db [:search :selected-results])))

(reg-sub
 :search/am-i-selected?
 :<- [:search/all-selected]
 (fn [all-selected [_ result]]
   (contains? all-selected result)))

(reg-sub
 :search/some-selected?
 :<- [:search/all-selected]
 (fn [all-selected _]
   (some? (seq all-selected))))

(reg-sub
 :search/selected-count
 :<- [:search/all-selected]
 (fn [all-selected _]
   (count all-selected)))

(reg-sub
 :search/selected-type
 :<- [:search/all-selected]
 (fn [all-selected _]
   (-> all-selected first :type)))

(reg-sub
 :search/total-results-count
 :<- [:search/full-results]
 (fn [full-results]
   (:count full-results)))

(reg-sub
 :search/results-count
 :<- [:search/results]
 (fn [results]
   (count results)))

(reg-sub
 :search/empty-filter?
 :<- [:search/results-count]
 :<- [:search/active-filter?]
 (fn [[results-count active-filter?]]
   (and (zero? results-count) active-filter?)))

(reg-sub
 :search/facet-names
 :<- [:search/facets-results]
 (fn [facets]
   (filter #(some? (seq (facets %))) (keys facets))))

(reg-sub
 :search/facet
 ;; A seq of the specified facet as MapEntry's in order of descending amounts.
 :<- [:search/facets-results]
 (fn [facets [_ facet-kw]]
   (sort-by val > (get facets facet-kw))))
