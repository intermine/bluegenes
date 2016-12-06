(ns redgenes.components.enrichment.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.results.subs]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]))

;;==============================TODO============================
;; 1. some enrichment widgets have filters! Add support for this
;;==============================================================


(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def sidebar-hover (reagent/atom false))

(defn popover-table []
  (let [values         (subscribe [:enrichment/summary-values])
        result         (first (:results @values))
        column-headers (:columnHeaders @values)]
    (fn [matches p-value]
      [:div.sidebar-popover
       [:table
        (into [:tbody]
              (map-indexed (fn [idx header]
                             [:tr.popover-contents.sidebar-popover
                              [:td.title (last (clojure.string/split header " > "))]
                              [:td.value (get result idx)]]) column-headers))]])))

(defn p-val-tooltip []
  [tooltip [:i.fa.fa-question-circle
            {:title          "The p-value is the probability that result occurs by chance, thus a lower p-value indicates greater enrichment."
             :data-trigger   "hover"
             :data-placement "bottom"}]])

(defn enrichment-result-row []
  (fn [{:keys [description matches identifier p-value matches-query] :as row}
       {:keys [pathConstraint] :as details}]
    [:li.enrichment-item
     {:on-mouse-enter (fn [] (dispatch [:enrichment/get-item-details identifier pathConstraint]))
      :on-click       (fn []
                        (dispatch [:enrichment/add-to-history row details]))}
     [:div.container-fluid
      [:div.row
       [:div.col-xs-8
        [popover
         [:span {:data-content   [popover-table matches p-value]
                 :data-placement "top"
                 :data-trigger   "hover"}
           ^{:key p-value}
            [:span description]]]]
       [:div.col-xs-4 [:span {:style {:font-size "0.8em"}} (.toExponential p-value 6)]]]]
     ]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description details]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(def enrichment-results-header
  [:div.enrichment-header
   [:div.container-fluid
   [:div.row
    [:div.col-xs-8 "Item"]
    [:div.col-xs-4.p-val "p-value" [p-val-tooltip]]]]])

(defn enrichment-results-preview []
  (let [page-state  (reagent/atom {:page 0 :show 5})
        text-filter (subscribe [:enrichment/text-filter])
        config (subscribe [:enrichment/enrichment-config])]
    (fn [[widget-name {:keys [results] :as details}]]
      [:div
        [:h4 {:class (if (empty? results) "greyout")}
        (get-in @config [widget-name :title])
        (if results
          [:span
           [:span (if results (str " (" (count results) ")"))]
           (if (< 0 (count results))
             [:span
              [:i.fa.fa-caret-left
               {:on-click (fn [] (swap! page-state update :page dec))
                :title "View previous 5 enrichment results"}]
              [:i.fa.fa-caret-right
               {:on-click (fn [] (swap! page-state update :page inc))
                :title "View next 5 enrichment results"}]])]
          [:span [:i.fa.fa-cog.fa-spin]])]
          (cond (seq (:results details)) enrichment-results-header)
       (into [:ul.enrichment-list]
         (map (fn [row] [enrichment-result-row row details])
          (take (:show @page-state)
            (filter
              (fn [{:keys [description]}]
                (has-text? @text-filter description))
              (drop (* (:page @page-state) (:show @page-state)) results)))))])))

(defn enrichment-results []
  (let [all-enrichment-results (subscribe [:enrichment/enrichment-results])]
    (fn []
      (if (nil? (vals @all-enrichment-results))
        ;; No Enrichment widgets available - only if there are no widgets for any of the datatypes in the columns. If there are widgets but there are 0 results, the second option below (div.demo) fires.
        [:div [:h4 "No Enrichment Widgets Available"]]
        [:div.demo
         [css-transition-group
          {:transition-name          "fade"
           :transition-enter-timeout 500
           :transition-leave-timeout 500}
          (map (fn [enrichment-response]
                 ^{:key (first enrichment-response)} [enrichment-results-preview enrichment-response])
               @all-enrichment-results)]]))))

(defn text-filter []
  (let [value (subscribe [:enrichment/text-filter])]
    [:input.form-control.input-lg
     {:type        "text"
      :value       @value
      :on-change   (fn [e]
                     (let [value (.. e -target -value)]
                       (if (or (= value "") (= value nil))
                         (dispatch [:enrichment/set-text-filter nil])
                         (dispatch [:enrichment/set-text-filter value]))))
      :placeholder "Filter..."}]))

(defn enrichment-settings []
  [:div
    [:div.enrichment-settings
      [:div.pval
        [:label "Max p-value" [p-val-tooltip]]
        [:select.form-control
          {:on-change #(dispatch [:enrichment/update-enrichment-setting :maxp (oget % "target" "value")])}
          [:option "0.05"]
          [:option "0.10"]
          [:option "1.00"]]]
      [:div.correction
        [:label "Test Correction"]
        [:select.form-control
          {:on-change #(dispatch [:enrichment/update-enrichment-setting :correction (oget % "target" "value")])}
          [:option "Holm-Bonferroni"]
          [:option "Benjamini Hochber"]
          [:option "Bonferroni"]
          [:option "None"]]]]
     [text-filter]
])

(defn enrichable-column-chooser [options active]
  (let [show-chooser? (reagent/atom false)]
    (fn []
      [:div.column-chooser [:a
        {:on-click (fn []
            (reset! show-chooser? (not @show-chooser?)))}
       "Change column (" (- (count options) 1) ")"]
       (cond @show-chooser?
         (into [:ul] (map (fn [option]
           (let [active? (= (:path option) (:path active))]
           [:li [:a
                 {:class (cond active? "active")
                  :key (:path option)
                  :on-click (fn []
                   (dispatch [:enrichment/update-active-enrichment-column option])
                   (reset! show-chooser? false))}
            (:path option)
            (cond active? " (currently active)")]]
          )) options)))
   ])))

(defn enrichable-column-displayer []
  (let [enrichable (subscribe [:enrichment/enrichable-columns])
        enrichable-options (flatten (vals @enrichable))
        multiple-options? (> (count enrichable-options) 1)
        active-enrichment-column (subscribe [:enrichment/active-enrichment-column])]
    [:div.enrichment-column-settings
      "Enrichment column: "
     [:span.the-column (first (:select (:query @active-enrichment-column)))]
     (cond multiple-options? [enrichable-column-chooser enrichable-options @active-enrichment-column])
     ]))

(defn enrich []
  (let [query-parts (subscribe [:results/query-parts])
        value       (subscribe [:enrichment/text-filter])]
    (fn []
      [:div.sidebar
       {:on-mouse-enter (fn [] (reset! sidebar-hover true))
        :on-mouse-leave (fn [] (reset! sidebar-hover false))}
       [:div
        [:h3.inline "Enrichment Statistics"]
        [enrichable-column-displayer]]
       [:div.expandable.present
        ; disable hover effects for now
        {:class (if (or @sidebar-hover @value) "present" "gone")}
[enrichment-settings]]
       [enrichment-results]])))
