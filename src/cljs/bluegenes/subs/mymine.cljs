(ns bluegenes.subs.mymine
  (:require [re-frame.core :refer [reg-sub]]
            [reagent.core :as r]
            [clojure.string :refer [split]]))


(defn branch?
  "Branch? fn for a tree-seq. True if this branch is open and has children."
  [m] (and (:open m) (contains? m :children)))

(defn children
  "Children fn for a tree-seq. Append the child's index to the collection of positions (trail)
  so we know the nested index of the child"
  [m] (map-indexed
                     (fn [idx child]
                       (assoc child :trail (vec (conj (:trail m) idx))))
                     (:children m)))
(defn flatten-tree
  "Flatten the nested tree structure of MyMine data into a list (depth first)"
  [m]
  (tree-seq branch? children m))

; MyMine data is a nested tree structure, however it's easier to display and sort when it's
; flattened into a list. Applying 'rest' removes the root folder
(reg-sub
  ::tree
  (fn [db]
    (-> db :mymine flatten-tree rest)))