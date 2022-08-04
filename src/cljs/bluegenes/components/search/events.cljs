(ns bluegenes.components.search.events
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [oops.core :refer [oget]]
            [goog.functions :refer [rateLimit]]
            [bluegenes.effects :refer [document-title]]))

(def results-batch-size
  "The amount of results we should fetch at a time."
  50)

(def results-max-size
  "The highest amount of results the webservice allows us to fetch at a time."
  100)

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

(reg-event-fx
 :search/selected-result
 (fn [{db :db} _]
   {:db (assoc db :quicksearch-selected-index -1)
    :track-event ["search" {:search_term (:search-term db)}]}))

(defn merge-filtered-facets
  "Sometimes we want to update our facets based on the currently active filter.
  When activating a filter, we want to keep our old facet for that filter, but
  update the other ones. This is achieved by calling this function normally.
  If we perform a new search with a filter active, we want to replace our old
  filtered facet, but keep the other ones. Specify the `:fresh` key for this."
  [filters old-facets facets & {:keys [fresh]}]
  (if fresh
    (let [new-facets (->> filters keys (select-keys facets))]
      (merge old-facets new-facets))
    (let [keep-facets (->> filters keys (select-keys old-facets))]
      (merge facets keep-facets))))

(reg-event-db
 :search/save-facets
 (fn [db [_ {:keys [facets]}]]
   (let [{filters :active-filters, old-facets :facets} (:search-results db)
         new-facets (merge-filtered-facets filters old-facets facets :fresh true)]
     (assoc-in db [:search-results :facets] new-facets))))

(reg-event-db
 :search/failure
 (fn [db [_ res]]
   (update db :search-results assoc
           :loading? false
           :error {:type "failure"
                   :message (get-in res [:body :error])})))

(reg-event-fx
 :search/save-results
 (fn [{db :db} [_
                {:keys [new-search? active-filter? removed-filter?]}
                {:keys [results facets totalHits]}]]
   (let [db (assoc-in db [:search-results :count] totalHits)
         service (get-in db [:mines (get db :current-mine) :service])
         search-term (get-in db [:search-results :keyword])]
     (cond
       (and new-search? active-filter?)
       ;; We had a filter activated when we performed a new search, so we want
       ;; to search again and only update the facets.  The reason for this is
       ;; that when doing a search with a filter activated, you will only get
       ;; the facets for that filter. To properly populate the facet list for
       ;; the new keyword, you will have to perform a regular search as well.
       {:db (update db :search-results assoc
                    :results results
                    :loading? false
                    :error nil
                    :facets facets)
        :im-chan {:chan (fetch/quicksearch service search-term {:size 0})
                  :on-success [:search/save-facets]}}

       active-filter?
       ;; We fetched the results for activating a filter, so leave the old
       ;; facet intact for the active filter, but update the other ones.
       (let [{filters :active-filters, old-facets :facets} (:search-results db)
             new-facets (merge-filtered-facets filters old-facets facets)]
         (cond-> {:db (update db :search-results assoc
                              :results results
                              :loading? false
                              :error nil
                              :facets new-facets)}
           ;; If we just removed a filter, we also need to do an empty search
           ;; to get the correct facets for the activated filters.
           removed-filter? (assoc :im-chan
                                  {:chan (fetch/quicksearch service search-term {:size 0})
                                   :on-success [:search/save-facets]})))

       :else
       ;; We fetched the results for a new plain search.
       {:db (update db :search-results assoc
                    :results results
                    :loading? false
                    :error nil
                    :facets facets)}))))

(defn active-filters->facet-query
  "To create the map that becomes the query strings we pass to our endpoint, we
  simply have to prepend `facet_` to the keys of our active filter map and turn
  the values into strings."
  [filters]
  (reduce-kv
   (fn [m k v]
     (assoc m
            (->> k name (str "facet_") keyword)
            (name v)))
   {}
   filters))

;; Dispatched when you enter the search page.
(reg-event-fx
 :search/begin-search
 (fn [{db :db} [_ search-term]]
   (let [new-search? (not= search-term (get-in db [:search-results :keyword]))]
     (if (and search-term new-search?)
       ;; By throwing the scroll information out of the window, we're making
       ;; sure the previous scroll position can't be restored. Mwuhahahaha!
       {:db (update db :search-results dissoc :scroll)
        :dispatch [:search/full-search search-term]}
       {}))))

(reg-event-fx
 :search/full-search
 [document-title]
 (fn [{db :db} [_ search-term removed-filter?]]
   (let [filters         (get-in db [:search-results :active-filters])
         connection      (get-in db [:mines (get db :current-mine) :service])
         new-search?     (not= search-term (get-in db [:search-results :keyword]))
         active-filters? (some? (seq filters))]
     {:db (-> db
              (assoc :search-term search-term)
              (assoc-in [:search-results :keyword] search-term)
              (cond-> new-search?
                (-> (update :search-results dissoc :results)
                    (assoc-in [:search-results :loading?] true))))
      :im-chan {:chan (fetch/quicksearch connection
                                         search-term
                                         (merge
                                          {:size results-batch-size}
                                          (when active-filters?
                                            (active-filters->facet-query filters))))
                :on-success [:search/save-results
                             {:new-search? new-search?
                              :active-filter? active-filters?
                              :removed-filter? removed-filter?}]
                :on-failure [:search/failure]}
      :track-event ["search" {:search_term search-term}]})))

(reg-event-fx
 :search/set-active-filter
 (fn [{db :db} [_ facet-name filter-name]]
   ;; There used to be code here that checked whether all the results of the
   ;; filter were already present in the results, and therefore didn't actually
   ;; trigger a request. Now that we have added more filters however, it's not
   ;; practical to take this into our own hands, so we just gotta take what the
   ;; server gives us.
   {:db (-> db
            (assoc-in [:search-results :active-filters facet-name] filter-name)
            (assoc-in [:search :selected-results] #{}))
    :dispatch [:search/full-search (get-in db [:search-results :keyword])]}))

(reg-event-fx
 :search/remove-active-filter
 (fn [{:keys [db]} [_ facet]]
   {:db (if facet
          (update-in db [:search-results :active-filters] dissoc facet)
          ;; Clear the entire active-filters map if no facet specified.
          (assoc-in db [:search-results :active-filters] {}))
    :dispatch [:search/full-search (-> db :search-results :keyword) true]}))

(reg-event-fx
 :search/to-results
 (fn [{:keys [db]}]
   (let [search-term  (get-in db [:search-results :keyword])
         all-selected (get-in db [:search :selected-results])
         object-type  (-> all-selected first :type)
         ids          (mapv :id all-selected)
         current-mine (:current-mine db)
         summary-fields (get-in db [:assets :summary-fields current-mine (keyword object-type)])]
     {:dispatch [:results/history+
                 {:source current-mine
                  :type :query
                  :intent :search
                  :value {:title (str "Selected " object-type " for '" search-term "'")
                          :from object-type
                          :select summary-fields
                          :where [{:path (str object-type ".id")
                                   :op "ONE OF"
                                   :values ids}]}}]})))

(reg-event-db
 :search/select-result
 (fn [db [_ result]]
   (update-in db [:search :selected-results] conj result)))

(reg-event-db
 :search/deselect-result
 (fn [db [_ result]]
   (update-in db [:search :selected-results] disj result)))

(reg-event-db
 :search/clear-selected
 (fn [db [_]]
   (update-in db [:search :selected-results] empty)))

(reg-event-fx
 :search/select-all-results
 (fn [{db :db} [_]]
   (let [{results-count :count results-fetched :results} (:search-results db)]
     ;; Should never really be less than, but keep it for safety!
     (if (<= results-count (count results-fetched))
       ;; We have already fetched all the results, so no need for a request.
       {:db (-> db
                (assoc-in [:search-results :loading-remaining?] false)
                (assoc-in [:search :selected-results] (set results-fetched)))}
       ;; Fetch remaining results as we don't know their IDs. As the search
       ;; webservice limits you to a maximum of `results-max-size` at a time,
       ;; this could lead to multiple subsequent requests.
       {:db (assoc-in db [:search-results :loading-remaining?] true)
        :dispatch [:search/remaining-results
                   {:on-success [:search/select-all-results]}]}))))

(declare scrolled-past?)

(reg-event-fx
 :search/save-more-results
 (fn [{db :db} [_ {:keys [results]}]]
   ;; You may think it very sneaky to dispatch an event from a setTimeout
   ;; callback, but this is to counter very eager scrollers whom sit at the
   ;; bottom of the page before rateLimit has a chance to reset. This means we
   ;; need to check their scroll position once we're sure the results have been
   ;; added to the DOM, lest we keep them waiting for the rest of their lives!
   (js/setTimeout (fn []
                    (when (scrolled-past?)
                      (dispatch [:search/more-results])))
                  1000)
   {:db (-> db
            (assoc-in [:search-results :loading-more?] false)
            (update-in [:search-results :results] into results))}))

(reg-event-fx
 :search/more-results
 (fn [{db :db} [_]]
   (let [{filters :active-filters, results :results, total :count, search-term :keyword
          :keys [loading-more? loading-remaining?]} (:search-results db)
         connection (get-in db [:mines (get db :current-mine) :service])]
     (if (and (< (count results) total)
              (not loading-more?)
              (not loading-remaining?))
       {:db (assoc-in db [:search-results :loading-more?] true)
        :im-chan {:chan (fetch/quicksearch connection
                                           search-term
                                           (merge
                                            {:size results-batch-size
                                             :start (count results)}
                                            (when (some? (seq filters))
                                              (active-filters->facet-query filters))))
                  :on-success [:search/save-more-results]}}
       {}))))

(reg-event-fx
 :search/save-remaining-results
 (fn [{db :db} [_ {:keys [on-success]} {:keys [results]}]]
   {:db (update-in db [:search-results :results] into results)
    :dispatch on-success}))

;; You could get duplicate results if you manage to click the "Select all"
;; button just as you scrolled to the bottom of the page, with more results to
;; load. Simplest way to fix would be to make [:search-results :results] a set.
;; I will not do this now, as it's such an edge case bug.
(reg-event-fx
 :search/remaining-results
 (fn [{db :db} [_ {:keys [on-success]}]]
   (let [{filters :active-filters, results :results
          total :count, search-term :keyword} (:search-results db)
         connection (get-in db [:mines (get db :current-mine) :service])]
     (if (< (count results) total)
       {:im-chan {:chan (fetch/quicksearch connection
                                           search-term
                                           (merge
                                            {:size results-max-size
                                             :start (count results)}
                                            (when (some? (seq filters))
                                              (active-filters->facet-query filters))))
                  :on-success [:search/save-remaining-results
                               {:on-success on-success}]}}
       {:dispatch on-success}))))

(defn scrolled-past?
  "Whether the user has scrolled past the point where we want to load more."
  []
  (let [scroll (.-scrollY js/window)
        height (- (oget js/document :body :scrollHeight) (.-innerHeight js/window))
        offset 500]
    (> (+ scroll offset) height)))

(let [limited-dispatch (rateLimit #(dispatch [:search/more-results]) 1000)]
  (defn endless-scroll-handler
    "Load more results when the user has scrolled far enough down the page.
    Use rateLimit to only dispatch the event once for an interval."
    [_e]
    (when (scrolled-past?)
      (limited-dispatch))))

(reg-event-db
 :search/save-scroll-position
 (fn [db [_ scroll]]
   (assoc-in db [:search-results :scroll] scroll)))

(reg-event-fx
 :search/restore-scroll-position
 (fn [{db :db} [_]]
   (let [scroll (get-in db [:search-results :scroll])]
     (when (number? scroll)
       (.scrollTo js/window 0 scroll))
     {})))

(reg-event-fx
 :search/start-scroll-handling
 (fn [_ [_]]
   (.addEventListener js/window "scroll" endless-scroll-handler)
   {}))

(reg-event-fx
 :search/stop-scroll-handling
 (fn [_ [_]]
   (.removeEventListener js/window "scroll" endless-scroll-handler)
   (dispatch [:search/save-scroll-position (.-scrollY js/window)])
   {}))
