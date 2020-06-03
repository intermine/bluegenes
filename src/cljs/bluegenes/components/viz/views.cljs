(ns bluegenes.components.viz.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.components.viz.cases :as cases]
            [oz.core :refer [vega-lite]]))

(def all-viz [{:config cases/config
               :query cases/query
               :viz cases/viz
               :key :cases}])

(defn main []
  (let [all-results @(subscribe [:viz/results])]
    (into [:div.viz]
          (for [{:keys [viz key]} all-viz]
            (when-let [results (get all-results key)]
              ^{:key (name key)}
              [:div.panel.panel-default
               [:div.panel-body
                [viz results]]])))))
