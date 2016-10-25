(ns redgenes.components.search.views
  (:require [reagent.core :as reagent]
            [clojure.string :as str]
            [redgenes.components.search.resultrow :as resulthandler]
            [redgenes.components.search.filters :as filters]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall oget]]
))

;;;;TODO: Cleanse the API/state arguments being passed around from the functions here. This is legacy of an older bluegenes history and module based structure.
;;;;TODO ALSO: abstract away from IMJS.
;;;;TODO: probably abstract events to the events... :D this file is a mixture of views and handlers, but really we just want views in the view file.

(def max-results 99);;todo - this is only used in a cond right now, won't modify number of results returned. IMJS was being tricky;

(defn sort-by-value [result-map]
 "Sort map results by their values. Used to order the category maps correctly"
 (into (sorted-map-by (fn [key1 key2]
                        (compare [(get result-map key2) key2]
                                 [(get result-map key1) key1])))
       result-map))

(defn results-handler [results]
   "Store results in local state once the promise comes back."
   (dispatch [:search/save-results results])
   )

(defn search
"search for the given term via IMJS promise. Filter is optional"
[& filter]
  (let [searchterm @(re-frame/subscribe [:search-term])
        mine (js/imjs.Service. (clj->js {:root @(subscribe [:mine-url])}))
        search {:q searchterm :Category filter}
        id-promise (-> mine (.search (clj->js search)))]
    (-> id-promise (.then
        (fn [results]
          (results-handler results))))))



(defn is-active-result? [result]
 "returns true is the result should be considered 'active' - e.g. if there is no filter at all, or if the result matches the active filter type."
   (or
     (= (:active-filter @(subscribe [:search/full-results])) (.-type result))
     (nil? (:active-filter @(subscribe [:search/full-results])))))

(defn count-total-results [state]
 "returns total number of results by summing the number of results per category. This includes any results on the server beyond the number that were returned"
 (reduce + (vals (:category (:facets state))))
 )

(defn count-current-results []
 "returns number of results currently shown, taking into account result limits nd filters"
 (count
   (remove
     (fn [result]
       (not (is-active-result? result))) (:results @(subscribe [:search/full-results])))))

(defn results-count []
 "Visual component: outputs the number of results shown."
   [:small " Displaying " (count-current-results) " of " (count-total-results @(subscribe [:search/full-results])) " results"])

(defn load-more-results [api search-term]
 (search (:active-filter @(subscribe [:search/full-results])))
 )

(defn results-display [api search-term]
 "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
 [:div.results
   [:h4 "Results for '" (:term @(subscribe [:search/full-results])) "'"  [results-count]]
   [:form
     (doall (let [state (subscribe [:search/full-results])
       ;;active-results might seem redundant, but it outputs the results we have client side
       ;;while the remote results are loading. Good for slow connections.
       active-results (filter (fn [result] (is-active-result? result)) (:results @state))
       filtered-result-count (get (:category (:facets @state)) (:active-filter @state))]
         ;;load more results if there are less than our preferred number, but more than
         ;;the original search returned
         (cond (and  (< (count-current-results) filtered-result-count)
                     (<= (count-current-results) max-results))
           (load-more-results api search-term))
         ;;output em!
         (for [result active-results]
           ^{:key (.-id result)}
           [resulthandler/result-row {:result result :state state :api api :search-term @search-term}])))]
  ])

(defn input-new-term []
  [:div
   (let [new-term (reagent/atom nil)]
    [:form.searchform {:on-submit
        (fn [e]
          (.preventDefault js/e) ;;don't submit the form, that just makes a redirect
          (.log js/console "%c@new-term" "color:hotpink;font-weight:bold;" (clj->js @new-term) )
          (cond (some? @new-term)
            (do
              (.log js/console "%cLet's go" "color:green;font-weight:bold;")
              (re-frame/dispatch [:search/set-search-term @new-term])
              (search)
              )))}
      [:input {:placeholder "Type a new search term here"
               :on-change
                (fn [e]
                  (let [input-val (oget e "target" "value")]
                    (.log js/console "%cinput-val" "color:blue;font-weight:bold;" (clj->js input-val))
                    (cond (not (clojure.string/blank? input-val))
                      (reset! new-term input-val))))}]
      [:button "Search"]])])


 (defn search-form [search-term api]
   "Visual form component which handles submit and change"
   [:div.search-fullscreen
    [:div.response
       [filters/facet-display (subscribe [:search/full-results]) api search @search-term]
       [results-display api search-term]]])

 (defn ^:export main []
   (let [global-search-term (re-frame/subscribe [:search-term])]
   (reagent/create-class
     {:reagent-render
       (fn render [{:keys [state upstream-data api]}]
         [search-form global-search-term api]
         )
       :component-will-mount (fn [this]
           (cond (some? @global-search-term)
               (search)))
       :component-will-update (fn [this]
         (cond (some? @global-search-term)
             (search)))
 })))
