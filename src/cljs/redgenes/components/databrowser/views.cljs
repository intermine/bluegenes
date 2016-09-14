(ns redgenes.components.databrowser.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
))

(def root (subscribe [:databrowser/root]))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [model (subscribe [:databrowser/whitelisted-model])]
      (into [:div.filter-by]
        (map (fn [[id vals]]
             [:p (get vals :displayName (:name vals))]
        ) @model))
  )])

(defn bubbles
  "Visual and interactive UI component allowing user to view model properties more visually than the text filters. " []
  [:div.bubbles [:h2 "Explore " @root ":"]
    (let [model (subscribe [:databrowser/whitelisted-model])
          r 50]
      (into [:svg {:version "1.1", :viewbox "0 0 500 500"}]
        (map-indexed (fn [index [id vals]]
          (let [r-positioned (* r (inc index))]
          [:g [:circle.bubble {:r r, :cy r-positioned, :cx r-positioned :class (str "type-" (:referencedType vals))}]
          [:text {:x r-positioned :y r-positioned :text-anchor "middle"} (get vals :displayName (:name vals))]]
      )) @model))
)])

(defn main []
  (fn []
    [:div.databrowser
      [visual-filter]
      [bubbles]
     [:div.preview [:h3 "Preview your data"]]
     ]))
