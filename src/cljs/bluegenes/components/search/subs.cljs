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
 :search/full-results
 (fn [db]
   (:search-results db)))

(reg-sub
 :search/active-filter
 :<- [:search/full-results]
 (fn [full-results]
   (:active-filter full-results)))

(reg-sub
 :search/highlight?
 :<- [:search/full-results]
 (fn [full-results]
   (:highlight-results full-results)))

(reg-sub
 :search/loading?
 :<- [:search/full-results]
 (fn [full-results]
   (:loading? full-results)))

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
 :search/total-results-count
 ;; total number of results by summing the number of results per category. This
 ;; includes any results on the server beyond the number that were returned
 :<- [:search/full-results]
 (fn [full-results]
   (reduce + (vals (:category (:facets full-results))))))

(reg-sub
 :search/active-results
 ;; results currently shown, taking into account result limits and filters.
 :<- [:search/full-results]
 :<- [:search/active-filter]
 (fn [[{:keys [results]} active-filter]]
   (if active-filter
     (filter #(= (:type %) (name active-filter)) results)
     results)))

(reg-sub
 :search/active-results-count
 :<- [:search/active-results]
 (fn [active-results]
   (count active-results)))

(reg-sub
 :search/empty-filter?
 :<- [:search/total-results-count]
 :<- [:search/active-results-count]
 (fn [[total-results current-results]]
   (and (zero? current-results)
        (pos? total-results))))
