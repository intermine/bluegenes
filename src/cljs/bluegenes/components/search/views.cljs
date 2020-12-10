(ns bluegenes.components.search.views
  (:require [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.search.resultrow :as resulthandler]
            [bluegenes.components.search.filters :as filters]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall oget]]
            [bluegenes.route :as route]
            [reagent.core :as reagent]))

(defn results-display
  "Iterate through results and output one row per result using result-row to format. Filtered results aren't output. "
  []
  (let [results         (subscribe [:search/results])
        active-filter?  (subscribe [:search/active-filter?])
        some-selected?  (subscribe [:search/some-selected?])
        search-keyword  (subscribe [:search/keyword])
        empty-filter?   (subscribe [:search/empty-filter?])
        total-count     (subscribe [:search/total-results-count])]
    (reagent/create-class
     {:component-did-mount
      (fn [_c]
        (dispatch [:search/restore-scroll-position]))
      :reagent-render
      (fn []
        [:div.results
         [:div.search-header
          [:h4 (str @total-count " results for '" @search-keyword "'")]

          (when (and @active-filter? @some-selected?)
            [:a.cta {:on-click (fn [e]
                                 (ocall e :preventDefault)
                                 (dispatch [:search/to-results]))}
             "View selected results in a table"])]
         [:form
          (doall (for [result @results]
                   ^{:key (:id result)}
                   [resulthandler/result-row {:result result :search-term @search-keyword}]))]

         (cond
           @empty-filter?
           [:div.empty-results
            "You might not be getting any results due to your active filters. "
            [:a {:on-click #(dispatch [:search/remove-active-filter])}
             "Click here"]
            " to clear the filters."]

           (zero? (count @results))
           [:div.empty-results
            "No results found. "])])})))

(defn input-new-term []
  (let [search-term @(subscribe [:search-term])]
    [:div
     [:form.searchform
      {:on-submit (fn [evt]
                    ;; Don't submit the form; that just makes a redirect.
                    (ocall evt :preventDefault)
                    (when (some? search-term)
                      ;; Set :suggestion-results to nil to force a :bounce-search next
                      ;; time we try to show suggestions, to avoid outdated results.
                      (dispatch [:handle-suggestions])
                      (dispatch [::route/navigate ::route/search nil {:keyword search-term}])))}
      [:input {:type "text"
               :value search-term
               :placeholder "Type a new search term here"
               :on-change #(dispatch [:search/set-search-term (oget % :target :value)])}]
      [:button "Search"]]]))

(defn search-form
  "Visual form component which handles submit and change"
  []
  (let [results (subscribe [:search/full-results])
        loading? (subscribe [:search/loading?])
        error (subscribe [:search/error])
        search-term (subscribe [:search-term])]
    [:div.search-fullscreen
     [input-new-term]
     (cond
       @error [:div.response.badresponse
               [:div.results
                [:div.search-header
                 [:h4 "Search returned an error"]]
                [:div.empty-results
                 [:code
                  (if-let [msg (-> @error :message not-empty)]
                    msg
                    "This is likely due to network issues. Please check your connection and try again later.")]]]]
       (some? (:results @results)) [:div.response
                                    [filters/facet-display results @search-term]
                                    [results-display]]
       :else [:div.noresponse
              [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]] "Try searching for something in the search box above - perhaps a gene, a protein, or a GO Term."])
     (when @loading?
       [:div.noresponse [loader "results"]])]))

(defn main []
  [search-form])
