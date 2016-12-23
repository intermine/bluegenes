(ns redgenes.components.ui.results_preview
  (:require [imcljs.path :as im-path]
            [oops.core :refer [oget]]
            [reagent.core :as reagent :refer [create-class]]
            [redgenes.components.loader :refer [loader]]
            [clojure.string :refer [split join]]))

(defn table-header []
  (fn [header]
    [:th (join " > " (take-last 2 (split header " > ")))]))

(defn table-row []
  (fn [row]
    (into [:tr]
          (map (fn [d]
                 [:td
                  (str (:value d))]) row))))

(defn preview-table []
  "Creates a dropdown for a query constraint.
  :query-results  The intermine model to use"
  (fn [& {:keys [query-results loading?]}]
    (if loading?
      [loader]
      [:table.table.small
       [:thead
        (into [:tr]
              (map (fn [h]
                     ^{:key h} [table-header h])
                   (:columnHeaders query-results)))]
       [:tbody
        (doall (map
                 (fn [r]
                   ^{:key (reduce str (map :id r))} [table-row r])
                 (:results query-results)))
        (if (> (:iTotalRecords query-results) 5)
          [:tr
           [:td
            {:col-span (count (:columnHeaders query-results))}
            (str "+ " (- (:iTotalRecords query-results) 5) " more results")]])]])))