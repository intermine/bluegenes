(ns re-frame-boiler.components.idresolver.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :idresolver/bank
  (fn [db]
    (-> db :idresolver :bank)))

(reg-sub
  :idresolver/resolving?
  (fn [db]
    (-> db :idresolver :resolving?)))

(reg-sub
  :idresolver/results
  (fn [db]
    (-> db :idresolver :results)))

(reg-sub
  :idresolver/results-item
  :< [:idresolver/results]
  (fn [results [_ input]]
    (filter (fn [[oid result]]
              (< -1 (.indexOf (:input result) input))) results)))

(reg-sub
  :idresolver/results-no-matches
  :< [:idresolver/results]
  (fn [results]
    (filter (fn [[oid result]]
              (= :UNRESOLVED (:status result))) results)))

(reg-sub
  :idresolver/results-matches
  :< [:idresolver/results]
  (fn [results]
    (filter (fn [[oid result]]
              (= :MATCH (:status result))) results)))

(reg-sub
  :idresolver/results-duplicates
  :< [:idresolver/results]
  (fn [results]
    (filter (fn [[oid result]]
              (= :DUPLICATE (:status result))) results)))
