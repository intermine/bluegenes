(ns bluegenes.pages.results.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :results/history
 (fn [db]
   (get-in db [:results :history])))

(reg-sub
 :results/history-index
 (fn [db]
   (get-in db [:results :history-index])))

(reg-sub
 :results/query
 (fn [db]
   (get-in db [:results :query])))

(reg-sub
 :results/service
 (fn [db]
   (get-in db [:results :service])))

(reg-sub
 :results/query-parts
 (fn [db]
   (get-in db [:results :query-parts])))

(reg-sub
 :results/enrichment-results
 (fn [db _]
   (get-in db [:results :enrichment-results])))

(reg-sub
 :results/enrichment-config
 (fn [db]
   (get-in db [:results :active-widgets])))

(reg-sub
 :results/active-enrichment-column
 (fn [db]
   (get-in db [:results :active-enrichment-column])))

(reg-sub
 :results/enrichable-columns
 (fn [db]
   (get-in db [:results :enrichable-columns])))

(reg-sub
 :results/text-filter
 (fn [db]
   (get-in db [:results :text-filter])))

(reg-sub
 :results/summary-values
 (fn [db]
   (get-in db [:results :summary-values])))

(reg-sub
 :results/package-for-table
 (fn [db]
   (let [{:keys [source value]} (get-in db [:results :package])]
     {:service (get-in db [:mines source :service])
      :query value})))

(reg-sub
 :results/are-there-results?
 (fn [db]
   (let [results (get-in db [:results :query])]
     (some? results))))

(reg-sub
 :results/historical-queries
 (fn [db]
   (sort-by (comp :last-executed second) > (seq (get-in db [:results :queries])))))

(reg-sub
 :results/current-list
 (fn [db]
   (let [current-list (get-in db [:assets :lists (get db :current-mine)])
         list-title (get-in db [:results :query :title])]
     (->> current-list (filter #(= list-title (:title %))) first))))