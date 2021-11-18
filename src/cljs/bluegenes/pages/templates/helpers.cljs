(ns bluegenes.pages.templates.helpers
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [clojure.string :as str]))

(defn categories-from-tags [tags]
  (->> tags
       (filter #(str/starts-with? % "im:aspect:"))
       (keep #(-> (re-matches #"im:aspect:(.*)" %) second not-empty))))

(defn categories [templates]
  (into []
        (comp (mapcat (comp categories-from-tags :tags val))
              (distinct))
        templates))

; Predicate function used to filter active constraints
(def not-disabled-predicate (comp (partial not= "OFF") :switched))

(defn remove-switchedoff-constraints
  "Filter the constraints of a query map and only keep those with a :switched value other than OFF"
  [query]
  (update query :where #(filterv not-disabled-predicate %)))

(defn clean-template-constraints
  [query]
  (update query :where
          (partial mapv (fn [const]
                          ; :description
                          (dissoc const :editable :switchable :switched :description)))))

(def prepare-template-query
  (comp clean-template-constraints remove-switchedoff-constraints))

