(ns bluegenes.components.enrichment.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.table :as table]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.sections.results.subs]
            [imcljs.path :as path]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget ocall]]))

;;==============================TODO============================
;; 1. some enrichment widgets have filters! Add support for this
;;==============================================================


(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def sidebar-hover (reagent/atom false))

(defn popover-table []
  (let [values (subscribe [:enrichment/summary-values])
        result (first (:results @values))
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
  [tooltip {:title
            "The p-value is the probability that result occurs by chance, thus a lower p-value indicates greater enrichment."}
   [:svg.icon.icon-question
    [:use {:xlinkHref "#icon-question"}]]])

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path path-constraint
                   :op "ONE OF"
                   :values (cond-> identifier (string? identifier) list)}))

(defn enrichment-result-row []
  (let [current-mine (subscribe [:current-mine-name])]
    (fn [{:keys [description matches identifier p-value matches-query] :as row}
         {:keys [pathConstraint] :as details}
         on-click
         selected?]
      [:li.enrichment-item
       {:on-mouse-enter (fn [] (dispatch [:enrichment/get-item-details identifier pathConstraint]))}
       [:div.container-fluid
        [:div.row
         [:div.col-xs-2
          [:input
           {:type "checkbox"
            :checked selected?
            :on-click (fn [e]
                        (ocall e :stopPropagation)
                        (on-click identifier))}]
          matches]
         [:div.col-xs-7
          [popover
           [:span {:data-content [popover-table matches p-value]
                   :data-placement "top"
                   :data-trigger "hover"}
            ^{:key p-value}
            [:a {:on-click (fn []
                             (dispatch [:results/history+ {:source @current-mine
                                                           :type :query
                                                           :value (assoc
                                                                    (build-matches-query
                                                                      (:pathQuery details)
                                                                      (:pathConstraint details)
                                                                      identifier)
                                                                    :title identifier)}]))} description]]]]
         [:div.col-xs-3 [:span {:style {:font-size "0.8em"}} (.toExponential p-value 6)]]]]])))

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
     [:div.col-xs-2 "Matches"]
     [:div.col-xs-7 "Item"]
     [:div.col-xs-3.p-val "p-value" [p-val-tooltip]]]]])

(defn enrichment-results-preview []
  (let [text-filter (subscribe [:enrichment/text-filter])
        config (subscribe [:enrichment/enrichment-config])
        selected (reagent/atom #{})
        current-mine (subscribe [:current-mine-name])]
    (fn [[widget-name {:keys [results] :as details}]]
      (let [on-click (fn [v]
                       (if (contains? @selected v)
                         (swap! selected (comp set (partial remove #{v})))
                         (swap! selected conj v)))
            filtered-results (filter
                               (fn [{:keys [description]}]
                                 (has-text? @text-filter description))
                               results)]
        [:div.sidebar-item
         [:h4 {:class (if (empty? results) "inactive")}
          (get-in @config [widget-name :title])
          (if results
            [:span (if results (str " (" (count results) ")"))]
            [:span [mini-loader "tiny"]])]
         (when (not-empty results)
           [:div.btn-toolbar
            [:button.btn.btn-default.btn-raised.btn-xs
             {:on-click (fn []
                          (dispatch [:results/history+
                                     {:source @current-mine
                                      :type :query
                                      :value (assoc
                                               (build-matches-query
                                                 (:pathQuery details)
                                                 (:pathConstraint details)
                                                 (or
                                                   ; Build a query for only the selected identifiers
                                                   (not-empty @selected)
                                                   ; ... unless it's empty, then use all filtered identifiers
                                                   (map :identifier filtered-results)))
                                               :title "Enrichment Results")}]))}
             "View"]])
         (cond (seq (:results details)) enrichment-results-header)
         (into [:ul.enrichment-list]
               (map (fn [row]
                      (let [selected? (contains? @selected (:identifier row))]
                        [enrichment-result-row row details on-click selected?]))
                    filtered-results))]))))

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
       :placeholder "Filter..."}]]))

(defn enrichment-settings []
  [:div.sidebar-item.enrichment-settings
   [:label.pval [:div.inline-label "Max p-value" [p-val-tooltip]]
    [:select.form-control
     {:on-change #(dispatch [:enrichment/update-enrichment-setting :maxp (oget % "target" "value")])}
     [:option "0.05"]
     [:option "0.10"]
     [:option "1.00"]]]

   [:label.correction "Test Correction"
    [:select.form-control
     {:on-change #(dispatch [:enrichment/update-enrichment-setting :correction (oget % "target" "value")])}
     [:option "Holm-Bonferroni"]
     [:option "Benjamini Hochberg"]
     [:option "Bonferroni"]
     [:option "None"]]]
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
       [:div.column-display [:svg.icon.icon-table [:use {:xlinkHref "#icon-table"}]]
        "Enrichment column: "
        [:div.the-column (path-to-last-two-classes model the-path)]]
       (cond multiple-options? [enrichable-column-chooser enrichable-options @active-enrichment-column])])))

(defn enrich []
  (let [query-parts (subscribe [:results/query-parts])
        value (subscribe [:enrichment/text-filter])]
    (fn []
      [:div.enrichment
       {:on-mouse-enter (fn [] (reset! sidebar-hover true))
        :on-mouse-leave (fn [] (reset! sidebar-hover false))}
       [:h3 "Enrichment "]
       [:a {:title "External link to enrichment documentation."
            :target "_blank"
            :href "http://intermine.readthedocs.io/en/latest/embedding/list-widgets/enrichment-widgets/"} "[How is this calculated? "
        [:svg.icon.icon-external [:use {:xlinkHref "#icon-external"}]] " ] "]
       [enrichable-column-displayer]

       [enrichment-settings]
       [enrichment-results]])))
