(ns bluegenes.subs.mymine
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [reagent.core :as r]
            [clojure.string :refer [split]]))

(defn branch?
  "Branch? fn for a tree-seq. True if this branch is open and has children."
  [m] (and (:open m) (contains? m :children)))

(defn children
  "Children fn for a tree-seq. Append the child's key to the collection of keys (trail)
  so we know the nested location of the child"
  [m]
  (map
    (fn [[key child]]
      (assoc child
        :size (count (:children child))
        :trail (vec (conj (:trail m) key))))
    (:children m)))

(defn flatten-tree
  "Flatten the nested tree structure of MyMine data into a list (depth first)"
  [m]
  (tree-seq branch? children m))

(reg-sub
  ::my-tree
  (fn [db]
    (get-in db [:mymine :tree])))

; Fill the [:root :public] folder with public lists
(reg-sub
  ::with-public
  (fn [] [(subscribe [::my-tree])
          (subscribe [:lists/filtered-lists])])
  (fn [[my-tree filtered-lists]]
    (assoc-in my-tree [:root :children :public :children]
              (into {} (map
                         (fn [l]
                           {(:id l) {:type :list
                                     :label (:title l)
                                     :id (:id l)
                                     :trail [0]}})
                         filtered-lists)))))

; MyMine data is a nested tree structure, however it's easier to display and sort when it's
; flattened into a list. Applying 'rest' removes the root folder
(reg-sub
  ::as-list
  (fn [] (subscribe [::with-public]))
  (fn [with-public]
    (-> with-public :root (assoc :fid :root :trail [:root]) flatten-tree rest)))


