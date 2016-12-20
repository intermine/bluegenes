(ns redgenes.sections.regions.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.sections.regions.graphs :as graphs]
            [redgenes.components.table :as table]
            [redgenes.components.loader :refer [loader]]
            [redgenes.sections.regions.events]
            [redgenes.sections.regions.subs]
            [redgenes.components.imcontrols.views :as im-controls]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget ocall]]))


(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn ex []
  (let [active-mine (subscribe [:current-mine])
        mines (subscribe [:mines])
        example-text (:regionsearch-example @active-mine)]
(clojure.string/join "\n" example-text)))

(def region-help-content-popover
  [:span "Genome regions in the following formats are accepted:"
   [:ul
    [:li [:span "chromosome:start..end, e.g. 2L:11334..12296"]]
    [:li [:span "chromosome:start-end, e.g. 2R:5866746-5868284 or chrII:14646344-14667746"]]
    [:li [:span "tab delimited"]]]])

(defn feature-branch []
  (let [settings (subscribe [:regions/settings])]
    (fn [[class-kw {:keys [displayName descendants] :as n}]]
      [:li {:class    (if (class-kw (:feature-types @settings)) "selected")
            :on-click (fn [e]
                        (.stopPropagation e)
                        (dispatch [:regions/toggle-feature-type n]))}
       (if (class-kw (:feature-types @settings))
         [:i.fa.fa-fw.fa-check-square-o]
         [:i.fa.fa-fw.fa-square-o])
       displayName
       (if-not (empty? descendants)
         (into [:ul.features-tree] (map (fn [d] [feature-branch d])) (sort-by (comp :displayName second) descendants)))])))

(defn feature-types-tree []
  (let [known-feature-types (subscribe [:regions/sequence-feature-types])
        settings            (subscribe [:regions/settings])
        ]
    (fn []
      (into [:ul.features-tree]
            (map (fn [f] [feature-branch f]) (sort-by (comp :displayName second) @known-feature-types))))))

(defn region-header
  "Header for each region. includes paginator and number of features."
  [{:keys [chromosome from to results] :as feature} paginator]
  [:h3 [:strong "Region: "]
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
  "Headerfor results table."
  []
  [:div.grid-3_xs-3
    [:div.col [:h4 "Feature"]]
    [:div.col [:h4 "Feature Type"]]
    [:div.col [:h4 "Location"]]])

(defn table-row
  "A single result row for a single region feature."
  [chromosome {:keys [primaryIdentifier class chromosomeLocation] :as result}]
  (let [model (subscribe [:model])]
    (.log js/console "%cresult" "color:hotpink;font-weight:bold;" (clj->js result))
  [:div.grid-3_xs-3
   [:div.col {:style {:word-wrap "break-word"}} primaryIdentifier]
   [:div.col (str (get-in @model [(keyword class) :displayName]))]
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
          [graphs/main feature]
          [:div.tabulated [table-header]
            (into [:div.results-body]
              (map (fn [result]  [table-row chromosome result])
                (take (:show @pager) (drop (* (:show @pager) (:page @pager)) results))))]]
        [:div.results.noresults [region-header chromosome from to] "No features returned for this region"]
        ))))

(defn organism-selection
  "UI component allowing user to choose which organisms to search. Defaults to all."
  []
  (let [settings  (subscribe [:regions/settings])]
    [:div [:label "Organism"]
      [im-controls/organism-dropdown
      {:label     (if-let [sn (get-in @settings [:organism :shortName])]
                    sn
                    "All Organisms")
       :on-change (fn [organism]
                    (dispatch [:regions/set-selected-organism organism]))}]])
  )

  ; Input box for regions
  (defn region-input-box
    "UI component allowing user to type in the regions they wish to search for"
    []
    (reagent/create-class
      (let [to-search (subscribe [:regions/to-search])
            results (subscribe [:regions/results])]
        {:reagent-render
          (fn []
            [:textarea.form-control
              {:rows        (if @results 3 6)
              :placeholder (str "Type chromosome coords here, or click [Show me an example] above.")
              :value       @to-search
              :on-change   (fn [e]
                (dispatch [:regions/set-to-search (oget e "target" "value")]))}])
         :component-did-mount (fn [this] (.focus (reagent/dom-node this)))})))

(defn clear-textbox []
  "Interactive UI component to clear any entered text present in the region input textarea."
  (let [to-search (subscribe [:regions/to-search])]
  [css-transition-group
   {:transition-name          "fade"
    :transition-enter-timeout 2000
    :transition-leave-timeout 2000
    :component "div"}
    (if @to-search
      [:div.clear-textbox
       ;;this is a fancy x-like character, aka &#10006; - NOT just x
       {:on-click #(dispatch [:regions/set-to-search nil])
        :title "Clear this textbox"} "✖"])]))

(defn region-input []
    [:div.region-input
      [:label "Regions to search "
        [popover [:i.fa.fa-question-circle
              {:data-content   region-help-content-popover
               :data-trigger   "hover"
               :data-placement "bottom"}]]]
        [:div.region-text
         [clear-textbox]
         [region-input-box]]
     ])

(defn checkboxes
  "UI component ot allow user to select which types of overlapping features to find"
  [to-search settings]

  (let [all-selected? (subscribe [:regions/sequence-feature-type-all-selected?])
        results (subscribe [:regions/results])]
  [:div.checkboxes
   [:label
    [:i.fa.fa-fw
     {:class (if @all-selected? "fa-check-square-o" "fa-square-o")
      :title (if @all-selected? "Deselect all" "Select all")
      :on-click (if @all-selected?
        #(dispatch [:regions/deselect-all-feature-types])
        #(dispatch [:regions/select-all-feature-types])
        )}] "Features to include"]
   ;; having the container around the tree is important because the tree is recursive
   ;; and we know for sure that the container is the final parent! :)
   [:div.feature-tree-container
    {:class (if @results "shrinkified")} [feature-types-tree]]]))

(defn input-section
  "Entire UI input section / top half of the region search"
  []
  (let [settings  (subscribe [:regions/settings])
  to-search (subscribe [:regions/to-search])]
  [:div.row.input-section
  ; Parameters section
  [:div.organism-and-regions
   [region-input]
   [organism-selection]
   [:button.btn.btn-primary.btn-raised.fattysubmitbutton
    {:disabled (or
                 (= "" @to-search)
                 (= nil @to-search)
                 (empty? (filter (fn [[name enabled?]] enabled?) (:feature-types @settings))))
     :on-click (fn [e] (dispatch [:regions/run-query])
                 (ocall (oget e "target") "blur"))
     :title "Enter something into the 'Regions to search' box or click on [Show me an example], then click here! :)"}
    "Search"]
   ]
   ; Results section
   [checkboxes to-search settings]
   ]))

(defn results-section []
  (let [results   (subscribe [:regions/results])
        loading? (subscribe [:regions/loading])]
  (if @loading? [loader "Regions"]
  [:div

     [:h2 (str "Results (" (apply + (map (comp count :results) @results)) ")")]
     [:div
      ; TODO: split this into a view per chromosome, otherwise it doesn't make sense on a linear plane
      #_[:div
         [graphs/main {:results (mapcat :results @results)}]]
      (into [:div.allresults]
        (map (fn [result]
          [result-table result]) @results))]])))

(defn main []
  (reagent/create-class
    {:component-did-mount #(dispatch [:regions/select-all-feature-types])
     :reagent-render
      (fn []
      [:div.container.regionsearch
        [:div.headerwithguidance
          [:h1 "Region Search"]
          [:a.guidance
           {:on-click #(dispatch [:regions/set-to-search (ex)])
          }
           "[Show me an example]"]]
       [input-section]
       [results-section]
       ])}))
