(ns re-frame-boiler.sections.objects.components.summary
  (:require [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]))

(defn field []
  (fn [k v]
    [:div.col-sm-6
     [:span.stressed (last (clojure.string/split k " > "))]
     [:span (if (nil? v) "N/A" (str v))]]))

(defn main []
  (fn [field-map]
    [:div
     [:h1 (str (:rootClass field-map) ": " (first (first (:results field-map))))]
     ;[:span (str field-map)]
     [:div.container (into [:div.row] (map (fn [f v] [field f v])
                                           (:columnHeaders field-map)
                                           (first (:results field-map))))]]))