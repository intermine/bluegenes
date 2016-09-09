(ns redgenes.components.search.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            ))

(reg-event-db
  :search/set-search-term
  (fn [db [_ search-term]]
    (assoc db :search-term search-term)))

(defn circular-index-finder
  "Returns the index of a result item, such that going down at the bottom loops to the top and vice versa."
  [direction]
  (let [result-index (subscribe [:quicksearch-selected-index])
        results-count (count @(subscribe [:suggestion-results]))
        next-number (if (= direction :next)
                      (inc @result-index)
                      (dec @result-index))
        looped-number (cond
                        (>= next-number results-count)
                        ;;if we go past the end, loop to the start
                          0
                        (< next-number 0)
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

(reg-event-db :search/reset-quicksearch
  (fn [db]
    (assoc db :suggestion-results nil)
))
