(ns bluegenes.components.search.filters
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [oops.core :refer [oget]]))

(defn remove-filter
  "the little x in the corner that allows you to remove filters, and its behaviour"
  [facet filter-name]
  [:a {:aria-label (str "Remove " filter-name " filter") ;;we need this to stop screen readers from reading the 'x' symbol out loud as though it was meaningful text
       :on-click (fn [e]
                   (.stopPropagation e) ;; if we don't do this the event bubbles to the tr click handler and re-applies the filter. lol.
                   (dispatch [:search/remove-active-filter facet]))}
   [:span.close "×"]]) ;;that's a cute little &times; to us HTML folk

(defn active-filter
  "Outputs which filter is active (if any) at the top of the filter section"
  [facet filt]
  [:div.active-filter filt
   [remove-filter facet filt]])

(defn controls []
  [:form.controls
   [:label
    [:input {:type "checkbox" :on-click
             (fn [e]
       ;;toggle highlight.
               (re-frame/dispatch [:search/highlight-results (oget e "target" "checked")]))}]
    [:span.checkbox-material [:span.check]]
    "Highlight search terms in results (experimental, may be sluggish)"]])

(defn facet-path->human
  "Takes a facet keyword path and converts it to a readable format.
  (E.g. `:organism.shortName` => `Organism`
        `:Category`           => `Category`)"
  [path]
  (-> path
      name
      (clojure.string/split #"\.")
      first
      (clojure.string/capitalize)))

(defn facet-filters
  "Displays the filters available for a facet."
  [facet]
  (let [active-filters @(subscribe [:search/active-filters])
        filters        @(subscribe [:search/facet facet])
        active?        #(= (get active-filters facet) %)]
    [:<>
     [:h5 (facet-path->human facet)]
     [:table
      (into [:tbody]
            (for [[filter-name amount] filters]
              ^{:key filter-name}
              [:tr {:on-click #(dispatch [:search/set-active-filter facet filter-name])
                    :class (when (active? filter-name) "active")}
               [:td.count.result-type {:class (str "type-" (name filter-name))} amount]
               [:td filter-name
                (when (active? filter-name)
                  [remove-filter facet filter-name])]]))]]))

(defn facet-display
  "Visual component which outputs the category filters."
  []
  (let [facet-names    @(subscribe [:search/facet-names])
        active-filters @(subscribe [:search/active-filters])]
    (when (seq facet-names)
      [:div.facets
       [:h4 "Filter by:"]
       [:h5 "Active filters: "]
       (if (seq active-filters)
         (into [:div.active]
               (for [[facet filt] active-filters]
                 ^{:key filt}
                 [active-filter facet filt]))
         [:div "None"])
       (into [:div]
             (for [facet facet-names]
               ^{:key facet}
               [facet-filters facet]))
       [controls]])))
