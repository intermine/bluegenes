(ns bluegenes.sections.regions.results
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.sections.regions.graphs :as graphs]
            [bluegenes.components.table :as table]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.sections.regions.events]
            [bluegenes.sections.regions.subs]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [oget ocall oset!]]))

(defn feature-to-uid [{:keys [chromosome from to results] :as feature}]
  (let [regions-searched (subscribe [:regions/regions-searched])]
    (if from
      ;;if we have all the details
      (str chromosome from to)
      ;;for empty results - combes back as just the chromosome name otherwise
      (let  [the-feature (first (filter
                (fn [x] (= (:chromosome x) feature)) @regions-searched))]
              (str (:chromosome the-feature) (:from the-feature) (:to the-feature))
))))

(defn region-header
  "Header for each region. includes paginator and number of features."
  [{:keys [chromosome from to results] :as feature} paginator]
   [:h3 {:id (feature-to-uid feature)} [:strong "Region: "]
    (if chromosome
      [:span chromosome " " from ".." to " "]
      [:span feature " "])
   [:small.features-count (count results) " overlapping features"]
   (cond (pos? (count results)) paginator)])

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
      :on-click (fn [] (swap! pager update :page inc))} "Next >"]]
  ))

(defn table-header
  "Header for results table."
  []
  [:div.grid-3_xs-3
    [:div.col [:h4 "Feature"]]
    [:div.col [:h4 "Feature Type"]]
    [:div.col [:h4 "Location"]]])

(defn table-row
  "A single result row for a single region feature."
  [chromosome {:keys [primaryIdentifier class chromosomeLocation objectId] :as result}]
  (let [model (subscribe [:model])
        current-mine (subscribe [:current-mine])
        the-type (get-in @model [(keyword class) :displayName])
        ]
  [:div.grid-3_xs-3.single-feature {:on-click #(navigate! (str "/reportpage/" (name (:id @current-mine)) "/" class "/" objectId))
       }
   [:div.col {:style {:word-wrap "break-word"}}
    primaryIdentifier]
   [:div.col the-type]
   [:div.col (str
               (get-in chromosomeLocation [:locatedOn :primaryIdentifier])
               ":"  (:start chromosomeLocation)
               ".." (:end chromosomeLocation))]]))

; Results table
(defn result-table
  "The result table for a region - all features"
  []
  (let [pager (reagent/atom {:show 20
                             :page 0})]
    (fn [{:keys [chromosome from to results] :as feature}]
      (if (pos? (count (:results feature)))
        [:div.results
          [region-header feature [table-paginator pager results]]
          ;[graphs/main feature]
          [:div.tabulated [table-header]
            (into [:div.results-body]
              (map (fn [result]  [table-row chromosome result])
                (take (:show @pager) (drop (* (:show @pager) (:page @pager)) (sort-by (comp :start :chromosomeLocation) results)))))]]
        [:div.results.noresults [region-header chromosome from to] "No features returned for this region"]
        ))))

(defn error-loading-results []
  [:div.results.error
   [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
   [:div.errordetails
    [:h3 "Houston, we've had a problem. "]
    [:p  "Looks like there was a problem fetching results."]
    [:ul
      [:li "Please check that your search regions are in the correct format."]
      [:li "Please check you're connected to the internet."]]]
   ])

(defn results-count-summary [results]
  (if (pos? (count results))
    (reduce (fn [new-div result]

      (let [num (count (:results result))
            feature (feature-to-uid result)
            ]
        (conj new-div
          [:span.results-count
           {:class (cond (zero? num) "noresults")
            :on-click (fn []
              (.scrollIntoView (.getElementById js/document feature) {:behavior "smooth"})
              (.scrollBy js/window 0 -80)
            )}
           [:strong (:chromosome result)] ": " num " results"]))
    ) [:div.results-counts [:span.skip-to "Skip to:"]] results)
    [:div]
    )
  )

(defn results-section []
  (let [results   (subscribe [:regions/results])
        loading? (subscribe [:regions/loading])
        error (subscribe [:regions/error])]
  (if @loading? [loader "Regions"]

   (if (not @error)
    [:div
      [:div.results-summary [:h2 "Results"] [results-count-summary @results]]
      (into [:div.allresults]
        (map (fn [result]
          [result-table result]) @results))]
     [error-loading-results]
     ))))
