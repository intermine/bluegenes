(ns bluegenes.components.templates.helpers
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))

(defn categories-from-template [[_ template-details]]
  (->> (:tags template-details)
       (filter (fn [tag] (re-find #"im:aspect:" tag)))
       (map (fn [tag] (last (clojure.string/split tag #"im:aspect:"))))))

(defn categories [templates]
  (into [] (distinct) (mapcat categories-from-template templates)))