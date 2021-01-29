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
