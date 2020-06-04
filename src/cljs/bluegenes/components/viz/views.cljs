(ns bluegenes.components.viz.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.components.viz.cases :as cases]
            [oz.core :refer [vega-lite]]))

(def all-viz [{:config cases/config
               :query cases/query
               :viz cases/viz
               :key :cases
               :package {:description "Developed for CovidMine. Shows a customizable plot and histogram for countries and timeline present in results."}}])

(defn main []
  (let [all-results @(subscribe [:viz/results])]
    (into [:div]
          (for [{:keys [viz key]} all-viz]
            (when-let [results (get all-results key)]
              ^{:key (name key)}
              [:div.viz
               [:div.panel.panel-default
                [:div.panel-body
                 [viz results]]]])))))
