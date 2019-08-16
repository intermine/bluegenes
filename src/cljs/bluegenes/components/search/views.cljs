(ns bluegenes.components.search.views
  (:require [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.search.resultrow :as resulthandler]
            [bluegenes.components.search.filters :as filters]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall]]
            [bluegenes.route :as route]))

;;;;TODO: abstract away from IMJS.
;;;;NOTES: This was refactored from bluegenes but might contain some legacy weird. If so I apologise.

(defn results-display
  "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
  []
  (let [results         (subscribe [:search/results])
        active-filter?  (subscribe [:search/active-filter?])
        some-selected?  (subscribe [:search/some-selected?])
        search-keyword  (subscribe [:search/keyword])
        search-term     (subscribe [:search-term])
        empty-filter?   (subscribe [:search/empty-filter?])
        total-count     (subscribe [:search/total-results-count])]
    [:div.results
     [:div.search-header
      [:h4 (str @total-count " results for '" @search-keyword "'")]

      (cond (and @active-filter? @some-selected?)
            [:a.cta {:on-click (fn [e]
                                 (ocall e :preventDefault)
                                 (dispatch [:search/to-results]))}
             "View selected results in a table"])]
     ;;TODO: Does this even make sense? Only implement if we figure out a good behaviour
     ;; select all search results seems odd, as does the whole page.
     ; [:div [:label "Select all" [:input {:type "checkbox"
     ;                  :on-click (dispatch [:search/select-all])}]]]
     [:form
      (doall (for [result @results]
               ^{:key (:id result)}
               [resulthandler/result-row {:result result :search-term @search-term}]))]

     (cond
       @empty-filter?
       [:div.empty-results
        "You might not be getting any results due to your active filters. "
        [:a {:on-click #(dispatch [:search/remove-active-filter])}
         "Click here"]
        " to clear the filters."]

       (zero? (count @results))
       [:div.empty-results
        "No results found. "])]))

(defn input-new-term []
  [:div
   (let [search-term @(subscribe [:search-term])]
     [:form.searchform
      {:on-submit
       (fn [evt]
         (ocall evt :preventDefault) ;;don't submit the form, that just makes a redirect
         (when (some? search-term)
           (dispatch [::route/navigate ::route/search nil {:keyword search-term}])))}
      [:input {:type "text"
               :value search-term
               :placeholder "Type a new search term here"
               :on-change #(dispatch [:search/set-search-term (-> % .-target .-value)])}]
      [:button "Search"]])])

(defn search-form
  "Visual form component which handles submit and change"
  [search-term]
  (let [results  (subscribe [:search/full-results])
        loading? (subscribe [:search/loading?])]
    [:div.search-fullscreen
     [input-new-term]
     (if (some? (:results @results))
       [:div.response
        [filters/facet-display results search-term]
        [results-display]]
       [:div.noresponse
        [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]] "Try searching for something in the search box above - perhaps a gene, a protein, or a GO Term."])
     (cond @loading? [:div.noresponse [loader "results"]])]))

(defn main []
  (let [search-term @(subscribe [:search-term])]
    [search-form search-term]))
