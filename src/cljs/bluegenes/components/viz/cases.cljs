(ns bluegenes.components.viz.cases
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [oops.core :refer [oget]]
            [goog.string :refer [parseInt]]
            [oz.core :refer [vega-lite]]))

(defn query [{{:keys [value]} :Cases}]
  {:from "Cases"
   :select ["date"
            "totalConfirmed"
            "totalDeaths"
            "newConfirmed"
            "newDeaths"
            "geoLocation.country"
            "geoLocation.state"]
   :where [{:path "Cases.id"
            :op "ONE OF"
            :values value}]})

(defn states-as-countries
  "Convert countries with states to be separate countries with the state in
  parentheses. Also removes the converted countries' total entries."
  [records]
  (let [countries-with-states (->> records
                                   (filter (comp :state :geoLocation))
                                   (map (comp :country :geoLocation))
                                   (set))]
    (->> records
         (remove (fn [{{:keys [country state]} :geoLocation}]
                   (and (contains? countries-with-states country)
                        (nil? state))))
         (map (fn [{{:keys [country state]} :geoLocation :as m}]
                (cond-> m
                  state (assoc-in [:geoLocation :country]
                                  (str country " (" state ")"))))))))


(defn top-x-countries
  "Filters away all but the top `x` countries with the highest return value
  for `keyfn`."
  [records x keyfn]
  (let [top-x (->> records
                   (group-by (comp :country :geoLocation))
                   (#(dissoc % "World"))
                   (map (juxt key
                              (comp #(reduce + %)
                                    #(map keyfn %)
                                    val)))
                   (sort-by second >)
                   (take x)
                   (map first)
                   (set))]
    (filter (comp top-x :country :geoLocation) records)))

(defn log-ready
  "Replaces zero or negative values under key `k` with 1,
  to allow log scales to be used."
  [records k]
  (map (fn [m]
         (update m k #(if (not (pos? %)) 1 %)))
       records))

(defn toggle-group [atom group-label props]
  [:div.toggle-group
   [:label {:style {:margin-left "0.5em"
                    :margin-right "0.5em"}}
    group-label]
   (into [:div.btn-group]
         (for [{:keys [label value active]} props]
           [:button.btn.btn-default.btn-sm
            {:type "button"
             :class (when (or active (= @atom value))
                      :active)
             :on-click #(reset! atom value)}
            (or label (name value))]))])

(defn number-input [atom label]
  [:div
   [:label {:style {:margin-left "0.5em"
                    :margin-right "0.5em"}}
    label]
   [:input.form-control
    {:type "number"
     :style {:height "30px" :width "80px" :display "inline-block"}
     :on-change #(when-let [x (parseInt (oget % :target :value))]
                   (when (pos? x)
                     (reset! atom x)))
     :value @atom}]])

;; - tooltips in histogram not working anymore (after adding selection: brush)
;; Suggestions:
;; - setting whether states should be included with or without country total, or excluded
;; - remove :style usage
(defn viz [results]
  (let [!top-x-count (r/atom 10)
        !mark-type (r/atom :line)
        !scale-type (r/atom :linear)
        !bin-scale (r/atom :linear)
        !y-field (r/atom :totalConfirmed)]
    (fn [results]
      [:div
       [:h4 (str "Showing top " @!top-x-count " countries with highest cases: " (name @!y-field))]
       [:div {:style {:display "flex"
                      :justify-content "space-evenly"}}
        [number-input !top-x-count
         "Top"]
        [toggle-group !mark-type
         "Plot"
         [{:value :line}
          {:value :area}]]
        [toggle-group !scale-type
         "Y-axis"
         (if (= @!mark-type :area)
           ;; Log scaling doesn't work for area charts.
           [{:value :linear :active true}]
           [{:value :linear}
            {:value :log}])]
        [toggle-group !bin-scale
         "Bins"
         [{:value :linear}
          {:value :log}]]
        [toggle-group !y-field
         "Value"
         [{:value :totalConfirmed}
          {:value :totalDeaths}
          {:value :newConfirmed}
          {:value :newDeaths}]]]
       [vega-lite
        {:data {:values (-> results
                            (states-as-countries)
                            (top-x-countries @!top-x-count @!y-field))}
         :autosize {:type "fit-x"
                    :contains "padding"}
         :vconcat [(merge
                    {:width "container"
                     :mark {:type (name @!mark-type) :tooltip true}
                     :encoding {:x {:field "date"
                                    :type "temporal"}
                                :y (merge {:field (name @!y-field)
                                           :type "quantitative"}
                                          (when (and (= @!scale-type :log)
                                                     (= @!mark-type :line))
                                            {:scale {:type "log" :base 2}}))
                                :color {:field "geoLocation.country"
                                        :type "nominal"}}
                     :selection {:brush {:encodings ["x"] :type "interval"}}}
                    (when (= @!scale-type :log)
                      ;; Replace zero and negative values with 1. In log scale,
                      ;; 0 becomes negative infinity and negative values have
                      ;; undefined behaviour.
                      (let [y-field (name @!y-field)
                            y (str "datum." y-field)]
                        {:transform [{:calculate (str "if ("y"<1, 1, "y")")
                                      :as y-field}]})))
                   {:width "container"
                    :mark {:type "bar" :tooltip true}
                    ;; We could support log scaling on the histogram's Y axis,
                    ;; but we'd have to disable stacking countries for that and
                    ;; the data may be faulty due to transform. Its usefulness
                    ;; is also questionable when we support logarithmic binning.
                    :encoding (if (= @!bin-scale :log)
                                {:x {:field "x1"
                                     :type "quantitative"
                                     :scale {:type "log" :base 10}
                                     :axis {:tickCount 5
                                            :title (str (name @!y-field) " (log binned)")}}
                                 :x2 {:field "x2"}
                                 :y {:aggregate "count"
                                     :type "quantitative"}
                                 :color {:field "geoLocation.country"
                                         :type "nominal"}}
                                {:x {:field (name @!y-field)
                                     :type "quantitative"
                                     :bin true}
                                 :y {:aggregate "count"
                                     :type "quantitative"}
                                 :color {:field "geoLocation.country"
                                         :type "nominal"}})
                    :transform (cond-> [{:filter {:selection "brush"}}]
                                 (= @!bin-scale :log)
                                 (into (let [x-field (name @!y-field)
                                             x (str "datum." x-field)]
                                         [{:filter (str x">0")}
                                          {:calculate (str "log("x")/log(10)")
                                           :as "log_x"}
                                          {:bin {:binned true :step 1} :field "log_x" :as "bin_log_x"}
                                          {:calculate "pow(10, datum.bin_log_x)"
                                           :as "x1"}
                                          {:calculate "pow(10, datum.bin_log_x_end)"
                                           :as "x2"}])))}]}]])))

(def config
  {:accepts ["ids"]
   :classes ["Cases"]
   :depends ["Cases"]
   :toolName {:human "Cases per-country plot and histogram"}})
