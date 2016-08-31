(ns redgenes.components.tooltip.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [dommy.core :refer-macros [sel sel1]]))

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
  []
  (reagent/create-class
    {:component-did-mount
     (fn [e]
       (.focus (sel1 (reagent/dom-node e) "input")))
     :reagent-render
     (fn [{:keys [content on-blur] :as data}]
       [:div.tooltip-bg.open
        [:div.fade {:on-click (fn [e]
                                (on-blur)
                                ;(closeme (.-parentNode (.-target e)))
                                )}]
        [:div.tooltip
         [:div.content content]
         [:div.close
          {:aria-label "Close"
           :on-click   (fn [e]
                         (closeme (.-parentNode (.-parentNode (.-target e))))
                         )} "x"]]])}))
