(ns redgenes.components.search.filters
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [reagent.core :as reagent]))

(defn filter-by [criterion state]
  "sets the active filter. Currently this is a single filter type; we may wish to handle multiples in the future"
  (swap! state assoc :active-filter criterion)
)

(defn is-active [name active]
  "returns whether a given filter is active"
  (= name active))

(defn remove-filter [filter-name state api search searchterm]
  "the little x in the corner that allows you to remove filters, and its behaviour"
  ;;we can probably tidy up the mess of args above.
  (fn [filter-name state]
    [:a {
      :aria-label (str "Remove " filter-name " filter") ;;we need this to stop screen readers from reading the 'x' symbol out loud as though it was meaningful text
      :on-click (fn [e]
        (.stopPropagation e) ;; if we don't do this the event bubbles to the tr click handler and re-applies the filter. lol.
        (swap! state dissoc :active-filter)
        (search searchterm api))}
      [:span.close "×"]])) ;;that's a cute little &times; to us HTML folk

(defn display-active-filter [active-filter state api search searchterm]
  "Outputs which filter is active (if any) at the top of the filter section"
  [:div.active
   [:h5 "Active filters: "]
    (if (some? active-filter)
      [:div.active-filter active-filter [remove-filter active-filter state api search searchterm]]
      [:div "None"])])

      (defn controls [state]
        [:form.controls
         [:label
          [:input {:type "checkbox" :on-click
                   (fn [e]
                     ;;toggle highlight.
                     (swap! state assoc :highlight-results (.-checked (.-target e))))
                   }]
          [:span.checkbox-material [:span.check]]
          "Highlight search terms in results (experimental, may be sluggish)"]
         ])

(defn facet-display [state api search searchterm]
  "Visual component which outputs the category filters."
  (let [facets (:facets @state) active (:active-filter @state)]
  (if (some? facets)
  [:div.facets
    [:h4 "Filter by:"]
      [display-active-filter active state api search searchterm]
      [:div
       ;;TODO: Re-implement this filter when we implement RESTful server-side filters
      ;  [:h5 "Organisms"]
      ;  [:table
      ;   (for [[name value] (:organisms facets)]
      ;     ^{:key name}
      ;     [:tr
      ;      [:td.count value]
      ;      [:td name]])]
       [:h5 "Categories"]
       [:table [:tbody
      (for [[name value] (:category facets)]
        ^{:key name}
        [:tr {
            :on-click (fn [e] (filter-by name state))
            :class (if (is-active name active) "active")}
         [:td.count.result-type {:class (str "type-" name)} value]
         [:td name (if (is-active name active)
           [remove-filter name state api search searchterm])]])]
       ]]
       [controls state]
   ])))
