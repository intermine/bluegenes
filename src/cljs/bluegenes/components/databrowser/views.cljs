(ns bluegenes.components.databrowser.views
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [reagent.core :as reagent]
            [imcljsold.counts :as counts]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [imcljsold.names :as names :refer [find-name find-type]]

))

(def pi (.-PI js/Math))

(defn short-quantity [val]
  (let [v (int val)]
    (cond
      (> v 10000)
        (str (int (/ v 1000)) "k")
      (> v 1000)
        (str (/ v 1000) "k")
      :else v)
    ))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/model-counts :human])]
      (into [:ul.filter-by]
        (map (fn [[id val]]
          [:li {:class (str "type-" (name id))
                :title (str (name id) " " val)} id " " [:span.quantity (short-quantity val)]]
        ) @model))
  )])

(defn bubbletext [location text]
  ;;the extra 4px are to offset the height of the text.
  [:g [:text.shadow {:x (:x location) :y (+ 4 (:y location)) :text-anchor "middle" :style {:stroke-width 2}} text]
  [:text.bubbletext {:x (:x location) :y (+ 4 (:y location)) :text-anchor "middle"} text]])

(defn bubbles
  "Visual and interactive UI component allowing user to view model properties more visually than the text filters. " []
    (let [root (subscribe [:databrowser/root])
          model (subscribe [:databrowser/model-counts :human])]
          [:div.bubbles [:h2 "Explore " @root ":"]
      (into [:svg.bubblegraph {:version "1.1"}]

        (map-indexed (fn [index [id val]]
          (let [location @(subscribe [:databrowser/node-locations id])]
          [:g {:class id}
           [:circle.bubble
            {:r (:r location)
             :cy (:y location)
             :cx (:x location)
             :class (str "type-" (name id))
            }]
            [bubbletext location id]]
      )) @model))
]))

(defn viewport-coords []
  {:y (.-clientHeight (.querySelector js/document ".bubblegraph"))
   :x (.-clientWidth (.querySelector js/document ".bubblegraph"))})

(defn main [] (reagent/create-class
  {:reagent-render
    (fn []
      [:div.databrowser
        [visual-filter]
        [bubbles]
      [:div.preview
        [:h3 "Preview your data"]
       [:div "Try clicking on a bubble on the left to get a preview of the data available."]
       [:button {:on-click #((dispatch [:databrowser/fetch-all-counts (viewport-coords)]))} "DO IT NOW" ]]
       ;[:div (map (fn [[k v]] [:div (clj->js k) v]) @(subscribe [:databrowser/model-counts :human]))]
     ])
   :component-did-mount #(dispatch [:databrowser/fetch-all-counts (viewport-coords)])

  })
)
