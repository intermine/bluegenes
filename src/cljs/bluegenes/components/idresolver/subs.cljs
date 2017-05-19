(ns bluegenes.components.idresolver.subs
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
  :idresolver/results-preview
  (fn [db]
    (-> db :idresolver :results-preview)))

(reg-sub
  :idresolver/fetching-preview?
  (fn [db]
    (-> db :idresolver :fetching-preview?)))

(reg-sub
  :idresolver/selected
  (fn [db]
    (get-in db [:idresolver :selected])))

(reg-sub
 :idresolver/selected-organism
 (fn [db]
  (get-in db [:idresolver :selected-organism :shortName])))


(reg-sub
 :idresolver/selected-object-type
 (fn [db]
   (let [object-type-default (get-in db [:mines (:current-mine db) :default-selected-object-type])
         selected (get-in db [:idresolver :selected-object-type])]
    (if (some? selected) selected object-type-default))))


(reg-sub
 :idresolver/object-types
 (fn [db]
   (let [current-mine (get-in db [:mines (get db :current-mine)])
         current-model (get-in current-mine [:service :model :classes])]
    (get-in db [:mines (:current-mine db) :default-object-types]))))


(reg-sub
  :idresolver/saved
  (fn [db [_ id]]
    (-> db :idresolver :saved (get id))))

(reg-sub
  :idresolver/everything
  (fn [db]
    (get-in db [:idresolver])))

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

(reg-sub
  :idresolver/results-type-converted
  :< [:idresolver/results]
  (fn [results]
    (filter (fn [[oid result]]
              (= :TYPE_CONVERTED (:status result))) results)))


(reg-sub
  :idresolver/results-other
  :< [:idresolver/results]
  (fn [results]
    (filter (fn [[oid result]]
              (= :OTHER (:status result))) results)))
