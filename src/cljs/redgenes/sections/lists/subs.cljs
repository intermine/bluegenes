(ns redgenes.sections.lists.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :refer [upper-case]]
            [redgenes.subs]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:title details)]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(reg-sub
  :lists/text-filter
  (fn [db]
    (get-in db [:lists :controls :filters :text-filter])))

(reg-sub
  :lists/sort-order
  (fn [db]
    (get-in db [:lists :controls :sort])))

(def sort-hierarchy [:title :count :created])

(defn build-comparer [[k v]]
  (comparator (fn [x y]
                (let [f (case v :asc < :desc > nil =)]
                  (cond
                    (nil? (get x k)) (= x y)
                    (string? (get x k)) (f (upper-case (get x k)) (upper-case (get y k)))
                    :else nil)))))

(reg-sub
  :lists/filtered-lists
  :<- [:lists]
  :<- [:lists/text-filter]
  :<- [:lists/sort-order]
  (fn [[all-lists text-filter sort-order] _]
    (->> all-lists
         (filter (partial has-text? text-filter))
         (sort (apply comp (map build-comparer sort-order))))))