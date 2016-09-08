(ns redgenes.components.databrowser.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
))

(defn main []
  (fn []
    [:div.databrowser
     [:div.filter [:h4 "Filter by: "]]
     [:div.bubbles [:h2 "Explore: "]
        [:div "Bubbles here."] ]
     [:div.preview [:h3 "Preview your data"]]
     ]))
