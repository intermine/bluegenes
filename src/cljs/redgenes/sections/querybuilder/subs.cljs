(ns redgenes.sections.querybuilder.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :qb/query-map
  (fn [db]
    (into (sorted-map) (get-in db [:qb :query-map]))))

(reg-sub
  :qb/query-constraints
  (fn [db]
    (get-in db [:qb :query-constraints])))