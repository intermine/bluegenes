(ns redgenes.components.tooltip.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]))

(defn closeme
  "pass the element that resolves to .tooltip-bg, and we'll nuke the node for you"
  [tooltip]
  (let [tooltip-parent (.-parentNode tooltip)]
    ;(.log js/console "%ctooltip" "color:hotpink;font-weight:bold;" (clj->js tooltip))
    ;(.log js/console "%ctooltip-parent" "color:hotpink;font-weight:bold;" (clj->js tooltip-parent))
    (.removeChild tooltip-parent tooltip)
    ))

(defn main
  "Creates a tooltip that pops up underneath the element it's created inside. the content argument can be any hiccup."
  [content]
    (fn []
      [:div.tooltip-bg.open
       [:div.fade {:on-click (fn [e]
        (closeme (.-parentNode (.-target e))))}]
       [:div.tooltip
       [:div.content content]
       [:div.close
        {:aria-label "Close"
         :on-click (fn [e]
            (closeme (.-parentNode (.-parentNode (.-target e))))
            )} "x"]]]
  ))
