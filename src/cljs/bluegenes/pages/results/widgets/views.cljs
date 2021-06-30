(ns bluegenes.pages.results.widgets.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.viz.common :refer [vega-lite]]
            [clojure.string :as str]
            [inflections.core :refer [plural]]
            [oops.core :refer [oget ocall]]
            [bluegenes.pages.results.enrichment.events :refer [build-matches-query]]))

(defn filter-display [& {:keys [widget-kw filterSelectedValue filters filterLabel]}]
  (let [filters (str/split filters #",")]
    [:label.widget-filter (str filterLabel ":")
     [:div
      (into [:select.form-control.input-sm
             {:on-change #(dispatch [:widgets/update-filter widget-kw (oget % :target :value)])
              :value filterSelectedValue}]
            (for [option filters]
              [:option option]))]]))

(defn widget [& {:keys [title description full-width? notAnalysed type
                        filterSelectedValue filters filterLabel
                        loading? values child
                        widget-kw]}]
  (conj [:div.widget.col-sm-12
         {:class (-> []
                     (into (when-not full-width? [:col-lg-6 :col-xl-4-workaround]))
                     (into (when loading? [:is-loading])))}
         [:h4 title]
         [:p {:dangerouslySetInnerHTML {:__html description}}]
         (if (pos? notAnalysed)
           [:p (str "Number of " (plural type) " in the table not analysed in this widget: ")
            [:strong notAnalysed]]
           [:p (str "All " (plural type) " in the table have been analysed in this widget.")])
         (when filters
           [filter-display
            :widget-kw widget-kw
            :filterSelectedValue filterSelectedValue
            :filters filters
            :filterLabel filterLabel])]
        (if (seq values)
          child
          [:p.failure (str "No results.")])))

(defn build-bar-query [query category series]
  (-> query
      (str/replace "%category" category)
      (str/replace "%series" series)
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

(let [counter (atom 0)]
  (defn handle-vega-signal! [{:keys [current-mine pathQuery title labels->values]} _signal-name signal-obj]
    (let [{:strs [domain type]} (js->clj signal-obj)
          type (labels->values type)]
      (dispatch [:results/history+
                 {:source current-mine
                  :type :query
                  :intent :widget
                  :value (assoc (build-bar-query pathQuery domain type)
                                :title (str title " " (swap! counter inc)))}]))))

(defn chart [widget-kw data & {:keys [full-width?]}]
  (let [{:keys [title description domainLabel rangeLabel chartType notAnalysed seriesValues seriesLabels type pathQuery
                filterSelectedValue filters filterLabel
                loading?]
         [labels & tuples] :results} data
        values (mapcat (fn [[domain & all-series]]
                         (map (fn [label value]
                                {:domain domain
                                 :type label
                                 :value value})
                              (rest labels)
                              all-series))
                       tuples ; Replace with below to see how it looks with more items.
                       #_(concat tuples
                                 (map #(update % 0 str " 2") tuples)))
        labels->values (zipmap (str/split seriesLabels #",") (str/split seriesValues #","))
        current-mine @(subscribe [:current-mine-name])]
    [widget
     :widget-kw widget-kw
     :title title
     :description description
     :full-width? full-width?
     :notAnalysed notAnalysed
     :type type
     :filterSelectedValue filterSelectedValue
     :filters filters
     :filterLabel filterLabel
     :loading? loading?
     :values values
     :child
     [vega-lite
      {:description description
       :height (case chartType
                 "BarChart" {:step 8}
                 "ColumnChart" nil)
       :data {:values values}
       :mark {:type "bar"
              :cursor "pointer"}
       :encoding {(case chartType
                    "BarChart" :row
                    "ColumnChart" :column)
                  {:field "domain"
                   :type "nominal"
                   :spacing 2
                   :title domainLabel
                   :header
                   (case chartType
                     "BarChart"
                     {:labelAngle 0
                      :labelAlign "left"
                      :labelOrient "left"
                      :labelLimit 100}
                     "ColumnChart"
                     {:labelAngle 45
                      :labelAlign "left"
                      :labelOrient "bottom"
                      :titleOrient "bottom"
                      :labelLimit 100})}

                  (case chartType
                    "BarChart" :x
                    "ColumnChart" :y)
                  {:field "value"
                   :type "quantitative"
                   :title rangeLabel
                   :axis (case chartType
                           "BarChart"
                           {:tickMinStep 1
                            :orient "top"}
                           "ColumnChart"
                           {:tickMinStep 1})}

                  (case chartType
                    "BarChart" :y
                    "ColumnChart" :x)
                  {:field "type"
                   :type "nominal"
                   :title nil
                   :sort (str/split seriesLabels #",")
                   :axis {:labels false
                          :ticks false}}

                  :color {:field "type"
                          :type "nominal"
                          :title nil
                          :scale {:domain (str/split seriesLabels #",")
                                  :scheme "category20"}
                          :legend {:direction "horizontal"
                                   :orient "top"}}

                  :tooltip [{:field "domain"
                             :type "nominal"}
                            {:field "type"
                             :type "nominal"}
                            {:field "value"
                             :type "quantitative"}]}}
      {:patch (fn [spec]
                (ocall spec [:signals :push]
                       (clj->js {:name "barClick"
                                 :on [{:events "rect:mousedown" :update "datum"}]}))
                spec)
       :callback (fn [result]
                   (ocall result [:view :addSignalListener]
                          "barClick" (partial handle-vega-signal! {:current-mine current-mine
                                                                   :pathQuery pathQuery
                                                                   :labels->values labels->values
                                                                   :title "Widget Results"})))}]]))

(defn piechart [widget-kw data & {:keys [full-width?]}]
  (let [{:keys [title description notAnalysed type rangeLabel
                filterSelectedValue filters filterLabel
                loading?]
         [_ & tuples] :results} data
        values (map #(zipmap [:category :value] %) tuples)]
    [widget
     :widget-kw widget-kw
     :title title
     :description description
     :full-width? full-width?
     :notAnalysed notAnalysed
     :type type
     :filterSelectedValue filterSelectedValue
     :filters filters
     :filterLabel filterLabel
     :loading? loading?
     :values values
     :child
     [vega-lite
      {:description description
       :title rangeLabel
       :data {:values values}
       :encoding {:theta {:field "value" :type "quantitative" :stack true}
                  :color {:field "category"
                          :type "nominal"
                          :legend {:title nil}
                          :scale {:scheme "category20c"}}}
       :layer [{:mark {:type "arc" :outerRadius 120}
                :encoding {:tooltip [{:field "category" :type "nominal"}
                                     {:field "value" :type "quantitative"}]}}
               {:mark {:type "text" :radius 100 :fill "white"}
                :encoding {:text {:field "value" :type "nominal"}}}]
       :view {:stroke nil}}]]))

(defn view-items! [{:keys [current-mine pathQuery pathConstraint identifiers title]}]
  (dispatch [:results/history+
             {:source current-mine
              :type :query
              :intent :widget
              :value (assoc (build-matches-query pathQuery pathConstraint identifiers)
                            :title title)}]))

(defn table-query-title [title columnTitle]
  (str title " widget: " columnTitle))

(defn table []
  (let [selected (reagent/atom #{})]
    (fn [widget-kw data & {:keys [full-width?]}]
      (let [{:keys [title description notAnalysed type columns pathQuery pathConstraint columnTitle]
             values :results} data
            ;; Uncomment me to see a tall table!
            ; values (take 20 (cycle values))
            current-mine @(subscribe [:current-mine-name])
            all-identifiers (mapv :identifier values)]
        [widget
         :title title
         :description description
         :full-width? full-width?
         :notAnalysed notAnalysed
         :type type
         :values values
         :child
         [:div
          [:button.btn.btn-default.btn-raised.btn-xs
           {:on-click #(view-items! {:current-mine current-mine
                                     :pathQuery pathQuery
                                     :pathConstraint pathConstraint
                                     :identifiers (if (empty? @selected)
                                                    all-identifiers
                                                    (vec @selected))
                                     :title (table-query-title title columnTitle)})}
           (if (empty? @selected) "View All" "View Selected")]
          [:div.table-widget
           [:table.table.table-condensed.table-striped
            [:thead
             (into [:tr
                    (let [all-identifiers-set (set all-identifiers)
                          is-checked (= @selected all-identifiers-set)
                          on-check (if is-checked
                                     #(swap! selected empty)
                                     #(reset! selected all-identifiers-set))]
                      [:th
                       [:input {:type "checkbox"
                                :checked is-checked
                                :on-click on-check}]])]
                   (for [col-header (str/split columns #",")]
                     [:th col-header]))]
            (into [:tbody]
                  (for [row values]
                    (into [:tr]
                          (let [{:keys [identifier descriptions matches]} row
                                is-checked (contains? @selected identifier)
                                on-check #(swap! selected (if is-checked disj conj) identifier)]
                            (concat
                             [[:td
                               [:input {:type "checkbox"
                                        :checked is-checked
                                        :on-click on-check}]]]
                             (map (fn [desc] [:td desc]) descriptions)
                             [[:td
                               [:a {:on-click #(view-items! {:current-mine current-mine
                                                             :pathQuery pathQuery
                                                             :pathConstraint pathConstraint
                                                             :identifiers identifier
                                                             :title (table-query-title title columnTitle)})}
                                matches]]])))))]]]]))))

(defn main []
  (let [widgets @(subscribe [:widgets/all-widgets])
        widgets (sort-by key
                         ;; Value is false if it failed, which we won't show.
                         (filter val widgets))]
    (when (seq widgets)
      [:div
       [:h3.widget-heading "Widgets"]
       [:div.widgets
        ;; Uncomment me to test the PieChart!
        #_[piechart :test-piechart {:chartType "PieChart",
                                    :description "Percentage of employees belonging to each company",
                                    :type "Employee",
                                    :list "Everyones-Favourite-Employees",
                                    :title "Company Affiliation",
                                    :rangeLabel "No. of employees",
                                    :notAnalysed 0,
                                    :results [["No. of employees"]
                                              ["Capitol Versicherung AG" 5]
                                              ["Dunder-Mifflin" 1]
                                              ["Wernham-Hogg" 1]]}]
        (doall (for [[widget-kw widget-data] widgets
                     :let [widget-comp (case (:chartType widget-data)
                                         ("BarChart" "ColumnChart") chart
                                         "PieChart" piechart
                                         ;; If chartType is missing, it's a table widget.
                                         table)
                           full-width? (= (count widgets) 1)]]
                 ^{:key widget-kw}
                 [widget-comp widget-kw widget-data :full-width? full-width?]))]])))
