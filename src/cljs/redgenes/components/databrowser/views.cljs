(ns redgenes.components.databrowser.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [model (subscribe [:model])
          whitelist (subscribe [:databrowser/whitelist])]
      (into [:div.filter-by]
        (map (fn [[id vals]]
               (cond (contains? @whitelist id)
             [:p id])
         ) @model)


            )
;      (.log js/console "%c@whitelist" "color:hotpink;font-weight:bold;"  @whitelist )
  )])

(defn main []
  (fn []
    [:div.databrowser
      [visual-filter]
     [:div.bubbles [:h2 "Explore: "]
        [:div "Bubbles here."]
      ]
     [:div.preview [:h3 "Preview your data"]]
     ]))
