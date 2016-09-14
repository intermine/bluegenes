(ns redgenes.components.databrowser.views
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [imcljs.counts :as counts]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [imcljs.names :as names :refer [find-name find-type]]

))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/whitelisted-model @root])]
      (into [:div.filter-by]
        (map (fn [[id vals]]
             [:p (find-name vals)]
        ) @model))
  )])

(defn bubbles
  "Visual and interactive UI component allowing user to view model properties more visually than the text filters. " []
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/whitelisted-model @root])
          r 50]
          [:div.bubbles [:h2 "Explore " @root ":"]
      (into [:svg {:version "1.1", :viewbox "0 0 500 500"}]
        (map-indexed (fn [index [id vals]]
          (let [r-positioned (* r (inc index))]
          [:g [:circle.bubble {:r r, :cy r-positioned, :cx r-positioned :class (str "type-" (find-type vals))}]
          [:text {:x r-positioned :y r-positioned :text-anchor "middle"} (find-name vals)]]
      )) @model))
]))

(defn main [] (reagent/create-class
  {:reagent-render
    (fn []
      [:div.databrowser
        [visual-filter]
        [bubbles]
      [:div.preview
        [:h3 "Preview your data"]]
        [:button {:on-click #(dispatch [:databrowser/fetch-all-counts])} "DO IT NOW" ]
     ])
   :component-did-mount #(dispatch [:databrowser/fetch-all-counts])

  })
)
