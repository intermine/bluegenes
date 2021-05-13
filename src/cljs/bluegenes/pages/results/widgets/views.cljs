(ns bluegenes.pages.results.widgets.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.components.viz.common :refer [vega-lite]]
            [clojure.string :as str]
            [inflections.core :refer [plural]]))

(defn widget [& {:keys [title description notAnalysed type values child]}]
  (conj [:div.widget.col-sm-12.col-md-6
         [:h4 title]
         [:p {:dangerouslySetInnerHTML {:__html description}}]
         (if (pos? notAnalysed)
           [:p (str "Number of " (plural type) " in the table not analysed in this widget: ")
            [:strong notAnalysed]]
           [:p (str "All " (plural type) " in the table have been analysed in this widget.")])]
        (if (seq values)
          child
          [:p.failure (str "No results.")])))

(defn chart [data]
  (let [{:keys [title description domainLabel rangeLabel chartType notAnalysed seriesLabels type]
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
                                 (map #(update % 0 str " 2") tuples)))]
    [widget
     :title title
     :description description
     :notAnalysed notAnalysed
     :type type
     :values values
     :child
     [vega-lite
      {:description description
       :height {:step 8}
       :data {:values values}
       :mark "bar"
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
                   :axis {:tickMinStep 1}}

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
                             :type "quantitative"}]}}]]))

(defn piechart [data]
  (let [{:keys [title description notAnalysed type rangeLabel]
         [_ & tuples] :results} data
        values (map #(zipmap [:category :value] %) tuples)]
    [widget
     :title title
     :description description
     :notAnalysed notAnalysed
     :type type
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

(defn table [data]
  (let [{:keys [title description notAnalysed type]
         values :results} data]
    [widget
     :title title
     :description description
     :notAnalysed notAnalysed
     :type type
     :values values
     :child
     ;; TODO make table widget
     [:pre (pr-str values)]]))

(defn main []
  (let [widgets @(subscribe [:widgets/all-widgets])]
    [:div.widgets
     ;; Uncomment me to test the PieChart!
     [piechart {:chartType "PieChart",
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
                                      table)]]
              ^{:key widget-kw}
              [widget-comp widget-data]))]))
