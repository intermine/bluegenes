(ns bluegenes.sections.querybuilder.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :refer [join blank?]]
            [bluegenes.sections.querybuilder.logic :as con-logic]))

(reg-sub
  :qb/query
  (fn [db]
    (into (sorted-map) (get-in db [:qb :qm]))))

(reg-sub
  :qb/query-constraints
  (fn [db]
    (get-in db [:qb :query-constraints])))

(reg-sub
  :qb/query-constraints
  (fn [db]
    (get-in db [:qb :query-constraints])))

(reg-sub
  :qb/query-is-valid?
  (fn [db]
    (get-in db [:qb :query-is-valid?])))

(defn flatten-query [[k value] total views]
  (let [new-total (conj total k)]
    (if-let [children (not-empty (select-keys value (filter (complement keyword?) (keys value))))] ; Keywords are reserved for flags
      (into [] (mapcat (fn [c] (flatten-query c new-total (conj views (assoc value :path new-total)))) children))
      (conj views (assoc value :path new-total)))))

(reg-sub
  :qb/flattened
  (fn [db]
    (distinct (flatten-query (first (get-in db [:qb :qm])) [] []))))

(reg-sub
  :qb/constraint-logic
  (fn [db]
    (join "" (drop-last (rest (apply vector (str (con-logic/vec->list (get-in db [:qb :constraint-logic])))))))))

(reg-sub
  :qb/root-class
  (fn [db]
    (get-in db [:qb :root-class])))

(reg-sub
  :qb/mappy
  (fn [db]
    (get-in db [:qb :mappy])))

(defn constraint-values
  "Walks down the query map and pulls all codes from constraints"
  [query]
  (map :value (mapcat :constraints (tree-seq map? vals query))))

(reg-sub
  :qb/constraint-value-count
  :<- [:qb/mappy]
  (fn [mappy]
    (count (remove blank? (constraint-values mappy)))))


(reg-sub
  :qb/preview
  (fn [db]
    (get-in db [:qb :preview])))

(reg-sub
  :qb/fetching-preview?
  (fn [db]
    (get-in db [:qb :fetching-preview?])))

(reg-sub
  :qb/im-query
  (fn [db]
    (get-in db [:qb :im-query])))

(reg-sub
  :qb/order
  (fn [db]
    (get-in db [:qb :order])))

(reg-sub
  :qb/menu
  (fn [db]
    (get-in db [:qb :menu])))

