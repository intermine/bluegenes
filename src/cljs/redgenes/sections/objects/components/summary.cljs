(ns redgenes.sections.objects.components.summary
  (:require [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]))

(defn field []
  (fn [k v]
    [:div.field
     [:div.field-label [:h4 (last (clojure.string/split k " > "))]]
     [:div.field-value
      (cond (nil? v) "N/A"
        :else (str v))]]))

(defn main []
  (fn [field-map]
    [:div.report-summary
      [:div
        [:h1 (str (:rootClass field-map) ": " (first (filter some? (first (:results field-map)))))]]
        (into [:div.fields] (map (fn [f v] [field f v])
                           (:columnHeaders field-map)
                           (first (:results field-map))))]))
