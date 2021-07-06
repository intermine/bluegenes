(ns bluegenes.pages.regions.results
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.pages.regions.graphs :as graphs]
            [bluegenes.components.table :as table]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.pages.regions.events :refer [prepare-export-query]]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [bluegenes.components.export-query :as export-query]
            [clojure.string :refer [split]]
            [oops.core :refer [oget ocall oset!]]
            [bluegenes.route :as route]
            [goog.functions :refer [debounce]]
            [imcljs.query :as im-query]
            [goog.dom :as gdom]
            [goog.fx.dom :as gfx]
            [goog.fx.easing :as geasing]
            [goog.style :as gstyle]))

(defn feature-to-uid [{:keys [chromosome from to results] :as feature}]
  (let [regions-searched (subscribe [:regions/regions-searched])]
    (if from
      ;;if we have all the details
      (str chromosome from to)
      ;;for empty results - combes back as just the chromosome name otherwise
      (let  [the-feature (first (filter
                                 (fn [x] (= (:chromosome x) feature)) @regions-searched))]
        (str (:chromosome the-feature) (:from the-feature) (:to the-feature))))))

(defn region-header
  "Header for each region. includes paginator and number of features."
  [{:keys [chromosome from to results] :as feature} paginator]
  [:h3 {:id (feature-to-uid feature)}
   [:strong "Region: "]
   [:span chromosome " " from ".." to " "]
   [:small.features-count (count results) " overlapping features"]
   (when (seq results) paginator)])

(defn table-paginator
  "UI component to switch between pages of results"
  [pager results]
  (let [page-count (int (.ceil js/Math (/ (count results) (:show @pager))))]
    [:div.pull-right.paginator
     [:button
      {:disabled (< (:page @pager) 1)
       :on-click (fn [] (swap! pager update :page dec))}
      "< Previous"]
     [:span.currentpage "Page " (inc (:page @pager)) " of " page-count]
     [:button
      {:disabled (< (count results) (* (:show @pager) (inc (:page @pager))))
       :on-click (fn [] (swap! pager update :page inc))} "Next >"]]))

(defn table-header
  "Header for results table."
  []
  [:div.grid-3_xs-3
   [:div.col [:h4 "Feature"]]
   [:div.col [:h4 "Feature Type"]]
   [:div.col [:h4 "Location"]]])

(def !dispatch
  ^{:doc
    "This dispatch is shared among all the row's mouseenter and mouseleave
    events. Without it, way too many events will fire causing slowdowns for
    large result sets."}
  (debounce dispatch 50))

(defn table-row
  "A single result row for a single region feature."
  [idx {:keys [symbol primaryIdentifier class chromosomeLocation objectId] :as result}]
  (let [model (subscribe [:model])
        current-mine (subscribe [:current-mine])
        the-type (get-in @model [(keyword class) :displayName])
        {:keys [start end locatedOn]} chromosomeLocation]
    [:a
     {:href (route/href ::route/report
                        {:mine (name (:id @current-mine))
                         :type class
                         :id objectId})
      :on-mouse-enter #(!dispatch [:regions/set-highlight idx
                                   {:chromosome (:primaryIdentifier locatedOn)
                                    :start start
                                    :end end}])
      :on-mouse-leave #(!dispatch [:regions/clear-highlight idx])}
     [:div.grid-3_xs-3.single-feature
      [:div.col.feature-name
       (when symbol [:strong symbol])
       primaryIdentifier]
      [:div.col the-type]
      [:div.col (str (:primaryIdentifier locatedOn) ":" start ".." end)]]]))

; Results table
(defn result-table
  "The result table for a region - all features"
  [idx]
  (let [pager (reagent/atom {:show 20
                             :page 0})
        service (subscribe [:active-service])
        subquery (subscribe [:regions/subquery idx])]
    (fn [idx {:keys [chromosome from to results] :as feature}]
      (if (seq (:results feature))
        [:div.results
         [region-header feature [table-paginator pager results]]
         [graphs/main idx feature]
         [:div.tabulated
          [table-header]
          (into [:div.results-body]
                ;; Note: If you change the sort function, make the same change
                ;; in bluegenes.pages.regions.graphs.
                (->> (sort-by (comp :start :chromosomeLocation) results)
                     (drop (* (:show @pager) (:page @pager)))
                     (take (:show @pager))
                     (map (fn [result]
                            [table-row idx result]))))]
         [:hr]
         [:div.results-footer
          [export-query/main (prepare-export-query @subquery)]
          [:button.btn.btn-default.btn-raised.btn-xs
           {:on-click #(dispatch [:regions/view-query @subquery feature])}
           "View in results table"]]]
        [:div.results.noresults [region-header chromosome from to] "No features returned for this region"]))))

(defn error-loading-results []
  [:div.results.error
   [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
   [:div.errordetails
    [:h3 "Houston, we've had a problem. "]
    [:p  "Looks like there was a problem fetching results."]
    [:ul
     [:li "Please check that your search regions are in the correct format."]
     [:li "Please check you're connected to the internet."]]]])

(defn scroll-into-view! [id]
  (when-let [elem (or (nil? id) (gdom/getElement id))]
    (let [current-scroll (clj->js ((juxt #(oget % :x) #(oget % :y))
                                   (gdom/getDocumentScroll)))
          target-scroll (if (nil? id)
                          #js [0 0] ; Scroll to top if no ID specified.
                          (clj->js ((juxt #(- (oget % :x) 110) #(- (oget % :y) 110))
                                    (gstyle/getRelativePosition elem (gdom/getDocumentScrollElement)))))]
      (doto (gfx/Scroll. (gdom/getDocumentScrollElement)
                         current-scroll
                         target-scroll
                         300
                         geasing/inAndOut)
        (.play)))))

(defn results-count-summary [results]
  (when (seq results)
    (into [:div.results-counts
           [:span.skip-to "Skip to:"]
           [:span.results-count
            {:on-click #(scroll-into-view! nil)}
            "Top"]]
          (for [result results
                :let [amount (count (:results result))
                      feature-id (feature-to-uid result)]]
            [:span.results-count
             {:class (when (zero? amount) :noresults)
              :on-click #(scroll-into-view! feature-id)}
             [:strong (:chromosome result)] ": " amount " results"]))))

(defn results-section []
  (let [results   (subscribe [:regions/results])
        loading? (subscribe [:regions/loading])
        error (subscribe [:regions/error])
        query (subscribe [:regions/query])]
    (fn []
      (cond
        @loading? [loader "Regions"]
        (not @error) [:div
                      (when (seq @results)
                        [:div.results-actions
                         [export-query/main (prepare-export-query @query)
                          :label "Export data for all features within all regions:"]
                         [:button.btn.btn-default.btn-raised.btn-xs
                          {:on-click #(dispatch [:regions/view-query @query])}
                          "View all in results table"]])
                      [:div.results-summary
                       [results-count-summary @results]]
                      (into [:div.allresults]
                            (map-indexed (fn [idx result]
                                           [result-table idx result])
                                         @results))]
        :else [error-loading-results]))))
