(ns bluegenes.components.search.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]))

(def max-results 99) ;;todo - this is only used in a cond right now, won't modify number of results returned. IMJS was being tricky;

(reg-event-db
 :search/set-search-term
 (fn [db [_ search-term]]
   (assoc db :search-term search-term)))

(defn circular-index-finder
  "Returns the index of a result item, such that going down at the bottom loops to the top and vice versa. Element -1 is 'show all'"
  [direction result-index results-count]
  (let [next-number   (if (= direction :next)
                        (inc result-index)
                        (dec result-index))
        looped-number (cond
                        (>= next-number results-count)
                        ;;if we go past the end, loop to the start
                        -1
                        (< next-number -1)
                        ;;if we go before the start, loop to the end.
                        (- results-count 1)
                        ;;if we fall through this far, next-number was in fact correct.
                        :else next-number)]
    looped-number))

(reg-event-db
 :search/move-selection
 (fn [{result-index :quicksearch-selected-index, results :suggestion-results, :as db}
      [_ direction]]
   (assoc db
          :quicksearch-selected-index
          (circular-index-finder direction result-index (count results)))))

(reg-event-db
 :search/reset-selection
 (fn [db _]
   (assoc db :quicksearch-selected-index -1)))

(defn sort-by-value
  "Sort map results by their values. Used to order the category maps correctly"
  [result-map]
  (into (sorted-map-by (fn [key1 key2]
                         (compare [(get result-map key2) key2]
                                  [(get result-map key1) key1])))
        result-map))

(reg-event-fx
 :search/save-results
 (fn [{db :db} [_
                {:keys [new-search? active-filter? facets-only?] :as predm}
                {:keys [results facets]}]]
   (cond
     facets-only?
     ;; We did a search *after* getting the results for a filter, so we want to
     ;; only add the facets.
     {:db (update db :search-results assoc
                  :facets {:organisms (sort-by-value (:organism.shortName facets))
                           :category (sort-by-value (:Category facets))})}

     (and new-search? active-filter?)
     ;; We had a filter activated when we performed a new search, so we want to
     ;; fetch the data with `facets-only?` to trigger the clause above.
     {:db (update db :search-results assoc
                  :results results
                  :loading? false)
      :im-chan {:chan (fetch/quicksearch (get-in db [:mines (get db :current-mine) :service])
                                         (get-in db [:search-results :keyword]))
                :on-success [:search/save-results (assoc predm :facets-only? true)]}}

     active-filter?
     ;; We fetched the results for activating a filter, so leave the old facets intact.
     {:db (update db :search-results assoc
                  :results results
                  :loading? false)}

     :else
     ;; We fetched the results for a new plain search.
     {:db (update db :search-results assoc
                  :results results
                  :loading? false
                  :highlight-results (:highlight-results (:search-results db))
                  :facets {:organisms (sort-by-value (:organism.shortName facets))
                           :category (sort-by-value (:Category facets))})})))

(reg-event-fx
 :search/full-search
 (fn [{db :db} [_ search-term]]
   (let [active-filter (some-> db :search-results :active-filter name)
         connection    (get-in db [:mines (get db :current-mine) :service])
         new-search?   (not= search-term (get-in db [:search-results :keyword]))]
     {:db (-> db
              (assoc :search-term search-term)
              (assoc-in [:search-results :keyword] search-term)
              (cond-> new-search?
                (-> (update :search-results dissoc :results)
                    (assoc-in [:search-results :loading?] true))))
      :im-chan {:chan (fetch/quicksearch connection
                                         search-term
                                         {:facet_Category active-filter})
                :on-success [:search/save-results
                             {:new-search? new-search?
                              :active-filter? (some? active-filter)}]}})))

(defn count-current-results
  "returns number of results currently shown, taking into account result limits and filters"
  [results active-filter]
  (count
   (if active-filter
     (filter #(= (:type %) active-filter) results)
     results)))

(reg-event-fx
 :search/set-active-filter
 (fn [{db :db} [_ filter-name]]
   (let [new-db (-> db
                    (assoc-in [:search-results :active-filter] filter-name)
                    (assoc-in [:search :selected-results] #{}))
         {:keys [results active-filter facets keyword]} (:search-results new-db)
         filtered-result-count          (get (:category facets) active-filter)
         active-results-count           (count-current-results results active-filter)
         more-filtered-results-to-show? (< active-results-count filtered-result-count)
         more-results-than-max?         (<= active-results-count max-results)]
     (cond-> {:db new-db}
       (and more-filtered-results-to-show? more-results-than-max?)
       ;; output the results we have client side alredy (ie if a non-filtered
       ;; search returns 100 results due to a limit, but indicates that there
       ;; are 132 proteins in total, we'll show all the proteins we have when we
       ;; filter down to just proteins, so the user might not even notice that
       ;; we're fetching the rest in the background.) while the remote results
       ;; are loading. Good for slow connections.
       (assoc :dispatch [:search/full-search keyword])))))

(reg-event-fx
 :search/remove-active-filter
 (fn [{:keys [db]}]
   {:db (update db :search-results dissoc :active-filter)
    :dispatch [:search/full-search (-> db :search-results :keyword)]}))

(reg-event-fx
 :search/to-results
 (fn [{:keys [db]}]
   (let [object-type    (get-in db [:search-results :active-filter])
         ids            (mapv :id (get-in db [:search :selected-results]))
         current-mine   (:current-mine db)
         summary-fields (get-in db [:assets :summary-fields current-mine object-type])]
     {:dispatch [:results/history+
                 {:source current-mine
                  :type :query
                  :value {:title "Search Results"
                          :from (name object-type)
                          :select summary-fields
                          :where [{:path (str (name object-type) ".id")
                                   :op "ONE OF"
                                   :values ids}]}}]})))

(reg-event-db
 :search/highlight-results
 (fn [db [_ highlight?]]
   (assoc-in db [:search-results :highlight-results] highlight?)))

(reg-event-db
 :search/select-result
 (fn [db [_ result]]
   (update-in db [:search :selected-results] conj result)))

(reg-event-db
 :search/deselect-result
 (fn [db [_ result]]
   (update-in db [:search :selected-results] disj result)))
