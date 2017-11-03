(ns bluegenes.components.thinker
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))


(defn main []
  (fn []
    [:div.thinking-container.fade-me-in
     {:style {:opacity 0.5
              :background-color "black"
              :position "fixed"
              :top 0
              :left 0
              :width "100%"
              :height "100%"
              :display "flex"
              :align-items "center"
              :justify-content "center"}}
     [:div.thinker-spinner]]))
