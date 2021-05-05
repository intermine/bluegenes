(ns bluegenes.pages.results.widgets.views
  (:require [re-frame.core :refer [subscribe]]
            [bluegenes.components.viz.common :refer [vega-lite]]))

(defn chart [data]
  (let [{:keys [title description domainLabel rangeLabel]
         [labels & tuples] :results} data
        values (mapcat (fn [[domain & all-series]]
                         (map (fn [label value]
                                {:domain domain
                                 :type label
                                 :value value})
                              (rest labels)
                              all-series))
                       tuples)]
    [:div.widget.col-sm-6
     [:h4 title]
     [:p description]
     [vega-lite
      (doto
       {:description description
        ; :width "container"
        :height {:step 8}
        ; :autosize {:type "fit-x"
        ;            :contains "padding"}
        :data {:values values}
        :mark {:type "bar"}
        :encoding {:row {:field "domain"
                         :type "nominal"
                         :spacing 2
                         :title domainLabel
                         :header {:labelAngle 0
                                  :labelAlign "left"
                                  :labelOrient "left"
                                  :labelLimit 100}}
                   :x {:field "value"
                       :type "quantitative"
                       :title rangeLabel
                       :axis {:tickMinStep 1}}
                   :y {:field "type"
                       :type "nominal"
                       :title nil
                       :axis {:labels false
                              :ticks false}}
                   :color {:field "type"
                           :type "nominal"
                           :title nil}
                   :tooltip [{:field "domain"
                              :type "nominal"}
                             {:field "type"
                              :type "nominal"}
                             {:field "value"
                              :type "quantitative"}]}}
       js/console.log)]]))

(defn table [data]
  [:h4 (:title data)])

(defn widget [data]
  (cond
    (and (= (:title data) "Gene Expression in the Fly (FlyAtlas)") ;; TODO DEBUG
         (:chartType data)) [chart data]
    :else [table data]))

(defn main []
  (let [widgets @(subscribe [:widgets/all-widgets])]
    [:div.widgets
     (doall (for [[widget-kw widget-data] widgets]
              ^{:key widget-kw}
              [widget widget-data]))]))
