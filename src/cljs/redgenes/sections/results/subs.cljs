(ns redgenes.sections.results.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))





(reg-sub
  :results/query
  (fn [db]
    (get-in db [:results :query])))

(reg-sub
  :results/query-parts
  (fn [db]
    (get-in db [:results :query-parts])))

(reg-sub
  :results/enrichment-results
  (fn [db _]
    (get-in db [:results :enrichment-results])))

(reg-sub
  :results/text-filter
  (fn [db]
    (get-in db [:results :text-filter])))