(ns redgenes.components.databrowser.views
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [imcljs.counts :as counts]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [imcljs.names :as names :refer [find-name find-type]]

))

(def pi (.-PI js/Math))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/model-counts :human])]
      (into [:div.filter-by]
        (map (fn [[id val]]
          [:p id " " [:span.quantity val]]
;          [:p (find-name vals)]
        ) @model))
  )])

(defn bubbletext [location text]
  [:g [:text.shadow {:x (:x location) :y (:y location) :text-anchor "middle" :style {:stroke-width 2}} text]
  [:text.bubbletext {:x (:x location) :y (:y location) :text-anchor "middle"} text]])

(defn bubbles
  "Visual and interactive UI component allowing user to view model properties more visually than the text filters. " []
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/model-counts :human])]
          [:div.bubbles [:h2 "Explore " @root ":"]
      (into [:svg {:version "1.1"}]

        (map-indexed (fn [index [id val]]
          (let [location @(subscribe [:databrowser/node-locations id])]
          [:g
           [:circle.bubble
            {:r (:r location)
             :cy (:y location)
             :cx (:x location)
             :class (str "type-" (name id))
            }]
            [bubbletext location id]]
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
       ;[:div (map (fn [[k v]] [:div (clj->js k) v]) @(subscribe [:databrowser/model-counts :human]))]
        [:button {:on-click #((dispatch [:databrowser/fetch-all-counts]))} "DO IT NOW" ]
     ])
   :component-did-mount #(dispatch [:databrowser/fetch-all-counts])

  })
)
