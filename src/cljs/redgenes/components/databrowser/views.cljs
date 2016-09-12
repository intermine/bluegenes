(ns redgenes.components.databrowser.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
))

(defn visual-filter
  "Visual and interactive UI component allowing the userto view selected model properties in a textual form." []
  [:div.filter [:h4 "Filter: "]
    (let [model (subscribe [:model])
          whitelist (subscribe [:databrowser/whitelist])]
      (into [:div] (map
       (fn [[k v]]
         [:div.drilldown k
        (.log js/console "%cv" "color:hotpink;font-weight:bold;" (clj->js k) (clj->js v))
          ])
      (keep
        (fn [[k v]] (if (contains? @whitelist k)
          k
          (.log js/console "%ck" "color:hotpink;font-weight:bold;" (clj->js k) (contains? @whitelist k) (type @whitelist) ))
        ) @model)))
      (.log js/console "%c@whitelist" "color:hotpink;font-weight:bold;"  @whitelist )
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
