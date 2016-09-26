(ns redgenes.components.querybuilder.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :query-builder/query
  (fn [db _]
    (:query (:query-builder db))))

(reg-sub
  :query-builder/io-query
  (fn [db _]
    (:io-query (:query-builder db))))

(reg-sub
  :query-builder/used-codes
  (fn [db _]
    (:used-codes (:query-builder db))))

(reg-sub
  :query-builder/queried?
  (fn [db _]
    (get-in db [:query-builder :queried?])))

(reg-sub
  :query-builder/count
  (fn [db _]
    (:count (:query-builder db))))

(reg-sub
  :query-builder/counting?
  (fn [db _]
    (:counting? (:query-builder db))))

(reg-sub
  :query-builder/current-constraint
  (fn [db _]
    (:constraint (:query-builder db))))