(ns bluegenes.subs.mymine
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [reagent.core :as r]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [clojure.string :refer [split]]))

; Thanks!
; https://groups.google.com/forum/#!topic/clojure/VVVa3TS15pU
(def asc compare)
(def desc #(compare %2 %1))

(def desc-date (fn [x y]
                 (when (and x y)
                   (let [x-parsed (tf/parse x)
                         y-parsed (tf/parse y)]
                     (t/before? x-parsed y-parsed)))))

(def asc-date (fn [y x]
                (when (and x y)
                  (let [x-parsed (tf/parse x)
                        y-parsed (tf/parse y)]
                    (t/before? x-parsed y-parsed)))))

(def folders>files (fn [x y]
                     (cond
                       (= x y) 0
                       (and (= :folder x) (not= :folder y)) 1
                       (and (= :folder x) (= :folder y)) -1)))

(defn compare-by [& key-cmp-pairs]
  (fn [x y]
    (loop [[k cmp & more] key-cmp-pairs]
      (let [result (cmp (k x) (k y))]
        (if (and (zero? result) more)
          (recur more)
          result)))))

;;;;;;;;;; tree-seq components to convert the nested MyMine JSON document

(defn branch?
  "Branch? fn for a tree-seq. True if this branch is open and has children."
  [m] (and (:open m) (contains? m :children)))

(defn children
  "Children fn for a tree-seq. Append the child's key to the collection of keys (trail)
  so we know the nested location of the child"
  [sort-info m]
  (map
    (fn [[key {file-type :file-type :as child}]]
      (assoc child
        :trail (vec (conj (:trail m) :children key))
        :level (cond-> (count (remove keyword? (:trail m))) (not= :folder file-type) inc)))
    (sort
      (compare-by
        (comp :file-type second) folders>files

        (comp (:key sort-info) second) (case (:type sort-info)
                                         :alphanum (if (:asc? sort-info) asc desc)
                                         :date (if (:asc? sort-info) asc-date desc-date)))
      (:children m))))

(defn flatten-tree
  "Flatten the nested tree structure of MyMine data into a list (depth first)"
  [m sort-info]
  (tree-seq branch? (partial children sort-info) m))

;;;;;;;;;; end of tree-seq components

(reg-sub
  ::sort-by
  (fn [db]
    (get-in db [:mymine :sort-by])))

(reg-sub
  ::my-tree
  (fn [db]
    (get-in db [:mymine :tree])))

(reg-sub
  ::selected
  (fn [db]
    (get-in db [:mymine :selected])))

; Fill the [:root :public] folder with public lists
(reg-sub
  ::with-public
  (fn [] [(subscribe [::my-tree])
          (subscribe [:lists/filtered-lists])])
  (fn [[my-tree filtered-lists]]
    (assoc-in my-tree [:root :children :public :children]
              (into {} (map
                         (fn [l]
                           {(:title l) (assoc l :file-type :list :read-only? true :label (:title l))})
                         ; TODO - rethink authorized flag
                         ; Assume that all unauthorized lists are public, but I bet this
                         ; isn't true if someone shares a list with you...
                         (filter (comp false? :authorized) filtered-lists))))))


(defn parse-dates [coll]
  (map
    (fn [item]
      (if-not (:dateCreated item)
        item
        (let [parsed (tf/parse (:dateCreated item))]
          (assoc item :date-created-obj parsed
                      :friendly-date-created (str
                                               (tf/unparse
                                                 (tf/formatter "MMM, dd")
                                                 parsed)
                                               " "
                                               (tf/unparse
                                                 (tf/formatter "YYYY")
                                                 parsed))))))
    coll))

; MyMine data is a nested tree structure, however it's easier to display and sort when it's
; flattened into a list. Applying 'rest' removes the root folder
(reg-sub
  ::as-list
  (fn [] [(subscribe [::with-public]) (subscribe [::sort-by])])
  (fn [[with-public sort-info]]
    (-> with-public :root (assoc :fid :root :trail [:root]) (flatten-tree sort-info) rest parse-dates)))

(reg-sub
  ::unfilled
  (fn [] [(subscribe [::as-list]) (subscribe [:lists/filtered-lists])])
  (fn [[as-list filtered-lists]]
    (let [my-lists (map :id (filter (comp true? :authorized) filtered-lists))
          filled   (remove nil? (map :id as-list))]
      ; TODO This is broken until https://github.com/intermine/intermine/pull/1633 is fixed
      (clojure.set/difference my-lists filled))))

(reg-sub
  ::context-menu-location
  (fn [db]
    (get-in db [:mymine :context-target])))

(reg-sub
  ::context-menu-target
  (fn [] [(subscribe [::with-public]) (subscribe [::context-menu-location])])
  (fn [[tree location]]
    (when location (assoc (get-in tree location) :trail location))))

(reg-sub
  ::edit-target
  (fn [] [(subscribe [::with-public]) (subscribe [::context-menu-location])])
  (fn [[tree location]]
    (assoc (get-in tree location) :trail location)))


