(ns bluegenes.components.search.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [oops.core :refer [ocall oget]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]
            [bluegenes.effects :as fx]
            ))


(def max-results 99) ;;todo - this is only used in a cond right now, won't modify number of results returned. IMJS was being tricky;

(reg-event-db
  :search/set-search-term
  (fn [db [_ search-term]]
    (assoc db :search-term search-term)))

(defn circular-index-finder
  "Returns the index of a result item, such that going down at the bottom loops to the top and vice versa. Element -1 is 'show all'"
  [direction]
  (let [result-index  (subscribe [:quicksearch-selected-index])
        results-count (count @(subscribe [:suggestion-results]))
        next-number   (if (= direction :next)
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
  (fn [db [_ {:keys [results facets] :as response}]]
    (if (some? (:active-filter (:search-results db)))
      ;;if we're returning a filter result, leave the old facets intact.
      (-> (assoc-in db [:search-results :results] results)
          (assoc-in [:search-results :loading?] false))
      ;;if we're returning a non-filtered result, add new facets to the atom
      (assoc db :search-results
                {
                 :results results
                 :loading? false
                 :highlight-results (:highlight-results (:search-results db))
                 :facets {
                          :organisms (sort-by-value (:organism.shortName facets))
                          :category (sort-by-value (:Category facets))}}))
    ))



(reg-event-fx
  :search/full-search
  (fn [{db :db}]
    (let [active-filter (:active-filter (:search-results db))
          connection    (get-in db [:mines (get db :current-mine) :service])]



      (-> {:db db}
          (assoc :im-chan {:chan (fetch/quicksearch connection
                                                    (:search-term db)
                                                    {:facet_Category active-filter})
                           :on-success [:search/save-results]})
          (assoc-in [:db :search-results :loading?] true)
          (cond-> (some? active-filter) (update-in db [:db :search-results] dissoc :results)))

      #_(if (some? active-filter)
          ;;just turn on the loader
          {:db (assoc-in db [:search-results :loading?] true)}
          ;;hide the old results and turn on the loader
          (let [resultless-db (assoc db :search-results (dissoc (:search-results db) :results))]
            {:db (assoc-in resultless-db [:search-results :loading?] true)})
          )

      )))

(reg-event-db ::major-success
              (fn [db [_ response]]
                (js/console.log "response" response)
                db))

(reg-event-db :search/reset-quicksearch
              (fn [db]
                (assoc db :suggestion-results nil)))

(defn is-active-result? [result active-filter]
  "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
  (or
    (= active-filter (oget result "type"))
    (nil? active-filter)))

(defn count-current-results [results filter]
  "returns number of results currently shown, taking into account result limits nd filters"
  (count
    (remove
      (fn [result]
        (not (is-active-result? result filter))) results)))

(reg-fx
  :load-more-results-if-needed
  ;;output the results we have client side alredy (ie if a non-filtered search returns 100 results due to a limit, but indicates that there are 132 proteins in total, we'll show all the proteins we have when we filter down to just proteins, so the user might not even notice that we're fetching the rest in the background.)
  ;;while the remote results are loading. Good for slow connections.
  (fn [search-results]
    (let [results                        (:results search-results)
          filter                         (:active-filter search-results)
          filtered-result-count          (get (:category (:facets search-results)) filter)
          more-filtered-results-to-show? (< (count-current-results results filter) filtered-result-count)
          more-results-than-max?         (<= (count-current-results results filter) max-results)]
      (cond (and more-filtered-results-to-show? more-results-than-max?)
            (dispatch [:search/full-search]))
      )))


(reg-event-fx
  :search/set-active-filter
  (fn [{:keys [db]} [_ filter]]
    (let [new-db (-> (assoc-in db [:search-results :active-filter] filter)
                     (assoc-in [:search :selected-results] #{}))]
      {:db new-db
       :load-more-results-if-needed (:search-results new-db)}
      )))

(reg-event-fx
  :search/remove-active-filter
  (fn [{:keys [db]}]
    {:db (assoc db :search-results (dissoc (:search-results db) :active-filter))
     :dispatch [:search/full-search]
     }
    ))

(reg-event-fx
  :search/to-results
  (fn [{:keys [db]}]
    (let [object-type    (get-in db [:search-results :active-filter])
          ids            (reduce (fn [result-ids result] (conj result-ids (oget result :id))) [] (get-in db [:search :selected-results]))
          current-mine   (:current-mine db)
          summary-fields (get-in db [:assets :summary-fields current-mine (keyword object-type)])]
      {:db db
       :dispatch [:results/history+
                  {:source current-mine
                   :type :query
                   :value {:title "Search Results"
                           :from object-type
                           :select summary-fields
                           :where [{:path (str object-type ".id")
                                    :op "ONE OF"
                                    :values ids}]}}]
       })))


(reg-event-db :search/highlight-results
              (fn [db [_ highlight?]]
                (assoc-in db [:search-results :highlight-results] highlight?)
                ))

(reg-event-db
  :search/select-result
  (fn [db [_ result]]
    (update-in db [:search :selected-results] conj result)
    ))

(reg-event-db
  :search/deselect-result
  (fn [db [_ result]]
    (update-in db [:search :selected-results] disj result)
    ))
