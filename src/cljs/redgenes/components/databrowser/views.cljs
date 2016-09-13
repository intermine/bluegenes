(ns redgenes.components.databrowser.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [model (subscribe [:databrowser/whitelisted-model])]
      (into [:div.filter-by]
        (map (fn [[id vals]]
             [:p (:name vals)]
        ) @model))
  )])

(defn bubbles
  "Visual and interactive UI component allowing user to view model properties more visually than the text filters. " []
  [:div.bubbles [:h2 "Explore: "]
    (let [model (subscribe [:databrowser/whitelisted-model])
          r 30]
          (into [:svg {:version "1.1", :viewbox "0 0 500 500"}]
            (map-indexed (fn [index [id vals]]
              (let [r-positioned (* r (inc index))]
              [:g [:circle {:r r, :cy r-positioned, :cx r-positioned :fill "rgba(80, 160, 240, 0.3)"}]
              [:text {:x r-positioned :y r-positioned :text-anchor "middle"} (:name vals)]]
            )) @model))



      )

])

(defn main []
  (fn []
    [:div.databrowser
      [visual-filter]
      [bubbles]
     [:div.preview [:h3 "Preview your data"]]
     ]))
