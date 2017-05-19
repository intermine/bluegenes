(ns bluegenes.sections.reportpage.components.summary
  (:require [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]))

(defn field []
  (fn [k v]
    [:div.field
     [:div.field-label [:h4 (last (clojure.string/split k " > "))]]
     [:div.field-value
      (cond (nil? v) "N/A"
        :else (str v))]]))

(defn is-entity-identifier?
  "simple string checking method to see if a given path type is appropriate for a report page title"
  [path]
  (or (clojure.string/ends-with? path "dentifier") ; i or I.
      (clojure.string/ends-with? path "symbol")))

(defn choose-title-column
  "Finds the first summary fields column that's an identifier or symbol. First field returned isn't always correct (in beany mines it can be the organism associated with the report page result, and not the identifier itself)."
  [field-map]
  (let [results (first (:results field-map))
        first-column (first (filter some? results))
        ;;build a vector of k/v pairs that could be suitable
        identifier-columns (reduce-kv (fn [new-vec index view]
          (if (and (is-entity-identifier? view) (some? (get results index)))
            (conj new-vec (get results index))
            new-vec)) [] (:views field-map))]
      (first identifier-columns)
))


(defn main []
  (fn [field-map]
    [:div.report-summary
      [:div
        [:h1 (str (:rootClass field-map) ": " (choose-title-column field-map))]]
        (into [:div.fields] (map (fn [f v] [field f v])
                           (:columnHeaders field-map)
                           (first (:results field-map))))]))
