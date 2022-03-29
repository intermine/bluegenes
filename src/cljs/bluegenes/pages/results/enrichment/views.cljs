(ns bluegenes.pages.results.enrichment.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.table :as table]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.pages.results.subs]
            [imcljs.path :as path]
            [bluegenes.components.bootstrap :refer [popover poppable tooltip]]
            [clojure.string :as str]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.components.ui.list_dropdown :refer [list-dropdown]]
            [goog.string :as gstring]))

(def css-transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransitionGroup))

(defn popover-table []
  (fn [{:keys [results columnHeaders] :as me}]
    [:div.sidebar-popover
     [:table.table.table-condensed.table-striped
      (into [:tbody]
            (map-indexed (fn [idx header]
                           [:tr.popover-contents.sidebar-popover
                            [:td.title (last (str/split header " > "))]
                            [:td.value (get (first results) idx)]]) columnHeaders))]]))

(defn p-val-tooltip []
  [poppable {:data "The p-value is the probability that result occurs by chance, thus a lower p-value indicates greater enrichment."
             :children [icon "question"]}])

(defn enrichment-result-row []
  (fn [{:keys [description matches identifier p-value matches-query] :as row}
       {:keys [pathConstraint] :as details}
       on-click
       selected?]
    [:li.enrichment-item
     {:on-mouse-enter (fn [] (dispatch [:enrichment/get-item-details identifier pathConstraint]))}
     [:div
      [:input
       {:type "checkbox"
        :checked selected?
        :on-click (fn [e]
                    (ocall e :stopPropagation)
                    (on-click identifier))}]]
     [:div
      (let [summary-value @(subscribe [:enrichment/a-summary-values identifier])]
        [poppable
         {:data (if summary-value
                  [popover-table @(subscribe [:enrichment/a-summary-values identifier])]
                  [:span "Loading"])
          :children [:a {:on-click #(dispatch [:enrichment/view-one-result details identifier])}
                     (str description " (" matches ")")]}])]
     [:div
      [:span.enrichment-p-value
       (.toExponential p-value 2)]]]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description details]
      (re-find (re-pattern (str "(?i)" (gstring/regExpEscape string))) description)
      false)
    true))

(defn enrichment-results-header []
  (fn [{:keys [on-click selected?]}]
    [:div.enrichment-header
     [:div
      {:style {:white-space "nowrap"}}
      [:input
       {:type "checkbox"
        :checked selected?
        :on-click (fn [e]
                    (ocall e :stopPropagation) (on-click))}]]
     [:div "Item (matches)"]
     [:div "p-value" [p-val-tooltip]]]))

(def results-to-show 5)

(defn enrichment-results-preview []
  (let [text-filter (subscribe [:enrichment/text-filter])
        config (subscribe [:enrichment/enrichment-config])
        selected (reagent/atom #{})
        show-more* (reagent/atom false)
        is-collapsed* (reagent/atom false)]
    (fn [[widget-name {:keys [results] :as details}]]
      (let [on-click (fn [v]
                       (if (contains? @selected v)
                         (swap! selected (comp set (partial remove #{v})))
                         (swap! selected conj v)))
            filtered-results (filter
                              (fn [{:keys [description]}]
                                (has-text? @text-filter description))
                              results)
            filtered-results-count (count filtered-results)
            identifiers (or
                         ;; Build a query for only the selected identifiers
                         (not-empty @selected)
                         ;; ... unless it's empty, then use all filtered identifiers
                         (map :identifier filtered-results))]
        [:div.sidebar-item

         [:div.enrichment-category
          {:class (when (empty? results) "inactive")}
          [:h4
           (get-in @config [widget-name :title])
           (cond
             results [:span (str " (" (count results) ")")]
             (string? details) [poppable {:data details
                                          :children [icon "warning"]}]
             :else [:span [mini-loader "tiny"]])]
          [:div.enrichment-category-right-side
           (when (not-empty results)
             [:<>
              [:button.btn.btn-default.btn-raised.btn-xs
               {:on-click #(dispatch [:enrichment/view-results details identifiers])}
               "View"]
              [:button.btn.btn-icon
               {:on-click #(dispatch [:enrichment/download-results details identifiers])}
               [icon "download"]]])
           [:button.btn.btn-link.toggle-enrichment
            {:on-click #(swap! is-collapsed* not)}
            [icon (if @is-collapsed*
                    "expand-folder"
                    "collapse-folder")]]]]

         (when (and (:filters details) (not @is-collapsed*))
           (let [{:keys [filterSelectedValue filters filterLabel]} details
                 filters (str/split filters #",")]
             [:label.enrichment-filter (str filterLabel ":")
              [:div
               (into [:select.form-control.input-sm
                      {:on-change #(dispatch [:enrichment/update-widget-filter widget-name (oget % "target" "value")])
                       :value filterSelectedValue}]
                     (for [option filters]
                       [:option option]))]]))

         (when-not @is-collapsed*
           (into [:ul.enrichment-list
                  (when (seq filtered-results)
                    [enrichment-results-header
                     {:selected? (= filtered-results-count (count @selected))
                      :on-click (fn []
                                  (if (empty? @selected)
                                    (reset! selected (set (map :identifier filtered-results)))
                                    (reset! selected #{})))}])]
                 (map (fn [row]
                        (let [selected? (contains? @selected (:identifier row))]
                          [enrichment-result-row row details on-click selected?]))
                      (if @show-more*
                        filtered-results
                        (take results-to-show filtered-results)))))

         (when-not @is-collapsed*
           (when (> filtered-results-count results-to-show)
             [:div.show-more-results
              [:button.btn.btn-link
               {:on-click #(swap! show-more* not)}
               (if @show-more* "Show less" "Show more")]]))]))))

(defn enrichment-results []
  (let [all-enrichment-results (subscribe [:enrichment/enrichment-results])
        loading-widget-types (subscribe [:enrichment/enrichment-widgets-loading?])]
    (fn []
      (if (nil? (vals @all-enrichment-results))
        ;; No Enrichment widgets available - only if there are no widgets for any of the datatypes in the columns. If there are widgets but there are 0 results, the second option below (div.demo) fires.
        [:div (if @loading-widget-types
                [:h4 "Finding enrichment widgets" [:span [mini-loader "tiny"]]]
                [:h4 "No Enrichment Widgets Available"])]
        [:div.demo
         [css-transition-group
          {:transition-name "fade"
           :transition-enter-timeout 500
           :transition-leave-timeout 500}
          (map (fn [enrichment-response]
                 ^{:key (first enrichment-response)} [enrichment-results-preview enrichment-response])
               @all-enrichment-results)]]))))

(defn text-filter []
  (let [value (subscribe [:enrichment/text-filter])]
    [:label.text-filter "Filter enrichment results"
     [:input.form-control
      {:type "text"
       :value @value
       :on-change (fn [e]
                    (let [value (.. e -target -value)]
                      (if (or (= value "") (= value nil))
                        (dispatch [:enrichment/set-text-filter nil])
                        (dispatch [:enrichment/set-text-filter value]))))
       :placeholder "Text to filter items"}]]))

(defn enrichment-settings []
  [:div.sidebar-item.enrichment-settings
   [:label.pval [:div.inline-label "Max p-value" [p-val-tooltip]]
    [:select.form-control
     {:on-change #(dispatch [:enrichment/update-enrichment-setting :maxp (oget % "target" "value")])
      :value @(subscribe [:enrichment/max-p-value])}
     [:option "0.05"]
     [:option "0.10"]
     [:option "1.00"]]]

   [:label.correction "Test Correction"
    [:select.form-control
     {:on-change #(dispatch [:enrichment/update-enrichment-setting :correction (oget % "target" "value")])
      :value @(subscribe [:enrichment/test-correction])}
     [:option "Holm-Bonferroni"]
     [:option "Benjamini Hochberg"]
     [:option "Bonferroni"]
     [:option "None"]]]

   (let [pop-value @(subscribe [:enrichment/background-population])
         widget-support? @(subscribe [:widget-support?])]
     [:div.population
      [:label "Background population"]
      [:div.population-controls
       [list-dropdown
        :value pop-value
        :lists @(subscribe [:current-lists])
        :restrict-type (:type @(subscribe [:enrichment/active-enrichment-column]))
        :on-change #(dispatch [:enrichment/update-enrichment-setting :population %])]
       (when pop-value
         [:button.btn.btn-link.population-clear
          {:title "Reset background population"
           :on-click #(dispatch [:enrichment/update-enrichment-setting :population nil])}
          [icon "close"]])]
      (when (and pop-value (not widget-support?))
        [:div.alert.alert-warning
         [:p "This mine is running an older InterMine version which does not support enrichment with background population in BlueGenes."]])])

   (when-let [message @(subscribe [:enrichment/enrichment-results-message])]
     [:div.alert.alert-info
      [:p message]])

   [text-filter]])

(defn path-to-last-two-classes [model this-path]
  (let [trimmed-path (path/trim-to-last-class model this-path)
        walk (path/walk model trimmed-path)
        n-to-take (- (count walk) 2)
        n-to-really-take (if (neg? n-to-take) 0 n-to-take)
        last-two-classes (subvec walk n-to-really-take)
        human-path (reduce (fn [newthing x]
                             (conj newthing (:displayName x))) [] last-two-classes)]
    (clojure.string/join " > " human-path)))

(defn enrichable-column-chooser [options]
  (let [show-chooser? (reagent/atom false)
        active (subscribe [:enrichment/active-enrichment-column])
        current-mine (subscribe [:current-mine])
        model (:model (:service @current-mine))]
    (fn []
      [:div.column-chooser [:a
                            {:on-click (fn []
                                         (reset! show-chooser? (not @show-chooser?)))}
                            (if @show-chooser?
                              "Select a column to enrich:"
                              [:span.change "Change column (" (- (count options) 1) ")"])]
       (cond @show-chooser?
             (into [:ul] (map (fn [option]
                                (let [this-path (:path option)
                                      active? (= this-path (:path @active))
                                      last-two (path-to-last-two-classes model this-path)]
                                  [:li [:a
                                        {:class (cond active? "active")
                                         :key this-path
                                         :on-click (fn []
                                                     (dispatch [:enrichment/update-active-enrichment-column option])
                                                     (reset! show-chooser? false))}
                                        last-two
                                        (cond active? " (currently active)")]])) options)))])))

(defn enrichable-column-displayer []
  (let [enrichable (subscribe [:enrichment/enrichable-columns])
        enrichable-options (flatten (vals @enrichable))
        multiple-options? (> (count enrichable-options) 1)
        active-enrichment-column (subscribe [:enrichment/active-enrichment-column])
        current-mine (subscribe [:current-mine])
        model (:model (:service @current-mine))
        the-path (first (:select (:query @active-enrichment-column)))]
    ; TODO why is the-path nil when the query root is, say, "DataSet"?
    ; This breaks the browser, so ignore nil paths for now. Issue #58
    (when the-path
      [:div.enrichment-column-settings.sidebar-item
       [:div.column-display
        "Enrichment column: "
        [:div.the-column (path-to-last-two-classes model the-path)]]
       (cond multiple-options? [enrichable-column-chooser enrichable-options @active-enrichment-column])])))

(defn enrichment-help-text []
  [poppable {:data "External link to enrichment documentation."
             :children [:a {:target "_blank"
                            :href "http://intermine.org/im-docs/docs/webapp/lists/list-widgets/enrichment-widgets/"}
                        [icon "info"]]}])

(defn enrich []
  [:div
   [:h3.enrichment-heading
    "Enrichment " [enrichment-help-text]]
   [:div.enrichment
    [enrichable-column-displayer]
    [enrichment-settings]
    [enrichment-results]]])
