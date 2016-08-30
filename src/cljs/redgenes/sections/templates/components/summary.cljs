(ns redgenes.sections.objects.components.summary
  (:require [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]))

(defn field []
  (fn [k v]
    [:div.col-sm-6.field-group
     [:div.row.field-value (if (nil? v) "N/A" (str v))]
     [:div.row.field-label (last (clojure.string/split k " > "))]]))

(defn main []
  (fn [field-map]
    [:div.container
     [:div.row
      [:div.col-xs-12
       [:h1 (str (:rootClass field-map) ": " (first (filter some? (first (:results field-map)))))]]]
     ;[:span (str field-map)]
     (into [:div.row] (map (fn [f v] [field f v])
                           (:columnHeaders field-map)
                           (first (:results field-map))))]))