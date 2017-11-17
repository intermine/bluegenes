(ns bluegenes.components.enrichment.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :enrichment/enrichment-results
  (fn [db _]
    (get-in db [:results :enrichment-results])))

(reg-sub
  :enrichment/enrichment-widgets-loading?
  (fn [db _]
    (get-in db [:results :enrichment-results-loading?])))

(reg-sub
  :enrichment/enrichment-config
  (fn [db]
    (get-in db [:results :active-widgets])))

(reg-sub
  :enrichment/active-enrichment-column
  (fn [db]
    (get-in db [:results :active-enrichment-column])))

(reg-sub
  :enrichment/enrichable-columns
  (fn [db]
    (get-in db [:results :enrichable-columns])))

(reg-sub
  :enrichment/text-filter
  (fn [db]
    (get-in db [:results :text-filter])))

(reg-sub
  :enrichment/summary-values
  (fn [db]
    (get-in db [:results :summary-values])))
