(ns bluegenes.pages.results.widgets.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.components.viz.common :refer [vega-lite]]
            [clojure.string :as str]))

(defn chart [data]
  (let [{:keys [title description domainLabel rangeLabel chartType notAnalysed seriesLabels]
         [labels & tuples] :results} data
        values (mapcat (fn [[domain & all-series]]
                         (map (fn [label value]
                                {:domain domain
                                 :type label
                                 :value value})
                              (rest labels)
                              all-series))
                       tuples)]
    [:div.widget.col-sm-12.col-md-6
     [:h4 title]
     [:p {:dangerouslySetInnerHTML {:__html description}}]
     (when (pos? notAnalysed)
       [:p "Number of Genes in the table not analysed in this widget: "
        [:strong notAnalysed]])
     (if (seq values)
       [vega-lite
        {:description description
         ; :width "container"
         :height {:step 8}
         ; :autosize {:type "fit-x"
         ;            :contains "padding"}
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
                                    :range {:scheme "category20"}}
                            :legend {:direction "horizontal"
                                     :orient "top"}}

                    :tooltip [{:field "domain"
                               :type "nominal"}
                              {:field "type"
                               :type "nominal"}
                              {:field "value"
                               :type "quantitative"}]}}]
       [:p.failure (str "No results.")])]))

(defn table [data]
  [:h4 (:title data)])

(defn widget [data]
  (cond
    (:chartType data) [chart data]
    :else [table data]))

(defn main []
  (let [widgets @(subscribe [:widgets/all-widgets])]
    [:div.widgets
     (doall (for [[widget-kw widget-data] widgets]
              ^{:key widget-kw}
              [widget widget-data]))]))
