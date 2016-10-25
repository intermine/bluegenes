(ns redgenes.components.search.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            ))

(reg-event-db
  :search/set-search-term
  (fn [db [_ search-term]]
    (assoc db :search-term search-term)))

(defn circular-index-finder
  "Returns the index of a result item, such that going down at the bottom loops to the top and vice versa. Element -1 is 'show all'"
  [direction]
  (let [result-index (subscribe [:quicksearch-selected-index])
        results-count (count @(subscribe [:suggestion-results]))
        next-number (if (= direction :next)
                      (inc @result-index)
                      (dec @result-index))
        looped-number (cond
                        (>= next-number results-count)
                        ;;if we go past the end, loop to the start
                          -1
                        (< next-number -1)
                        ;;if we go before the start, loop to the end.
                          (- results-count 1)
                        ;;if we fall through this far, next-number was in fact correct.
                        :else next-number
                      )]
  looped-number))

(reg-event-db
  :search/move-selection
  (fn [db [_ direction-to-move]]
      (assoc db :quicksearch-selected-index (circular-index-finder direction-to-move))
))

(reg-event-db
  :search/reset-selection
  (fn [db [_ direction-to-move]]
      (assoc db :quicksearch-selected-index -1)
))


(defn sort-by-value [result-map]
 "Sort map results by their values. Used to order the category maps correctly"
 (into (sorted-map-by (fn [key1 key2]
                        (compare [(get result-map key2) key2]
                                 [(get result-map key1) key1])))
       result-map))

(reg-event-db
 :search/save-results
 (fn [db [_ results]]
  (let [searchterm (:search-term db)
        filter (:search-filter db)] ;;TODO
     (if (:active-filter (:search-results db))
       ;;if we're resturning a filter result, leave the old facets intact.
       (assoc db :search-results (.-results results))
       ;;if we're returning a non-filtered result, add new facets to the atom
       (assoc db :search-results
         {
         :results  (.-results results)
         :term searchterm
         :highlight-results (:highlight-results (:search-results db))
         :facets {
           :organisms (sort-by-value (js->clj (aget results "facets" "organism.shortName")))
           :category (sort-by-value (js->clj (aget results "facets" "Category")))}}))
     )
))

(defn search
 "search for the given term via IMJS promise. Filter is optional"
 [& filter]
   (let [searchterm @(re-frame/subscribe [:search-term])
         mine (js/imjs.Service. (clj->js {:root @(subscribe [:mine-url])}))
         search {:q searchterm :Category filter}
         id-promise (-> mine (.search (clj->js search)))]
     (-> id-promise (.then
         (fn [results]
           (dispatch [:search/save-results results]))))))

(reg-event-fx :search/full-search
  (fn [{db :db} [_ filter]]
    (.log js/console "%cSearching" "background:#ddd;color:#000;order-left:solid 3px cornflowerblue;" )
    (search filter)
{:db db}))

(reg-event-db :search/reset-quicksearch
  (fn [db]
    (assoc db :suggestion-results nil)
))
