(ns redgenes.sections.querybuilder.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :qb/query-map
  (fn [db]
    (into (sorted-map) (get-in db [:qb :query-map]))))

(reg-sub
  :qb/qm
  (fn [db]
    (into (sorted-map) (get-in db [:qb :qm]))))

(reg-sub
  :qb/query-constraints
  (fn [db]
    (get-in db [:qb :query-constraints])))

(reg-sub
  :qb/invisible-constraints
  :<- [:qb/query-map]
  :<- [:qb/query-constraints]
  (fn [[query-map query-constraints]]
    (.log js/console "QM" query-map)
    (.log js/console "QC" query-constraints)
    "Test"
    ))