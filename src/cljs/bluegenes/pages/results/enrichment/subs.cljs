(ns bluegenes.pages.results.enrichment.subs
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
 :enrichment/enrichment-results-message
 (fn [db]
   (get-in db [:results :enrichment-results-message])))

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

(reg-sub
 :enrichment/a-summary-values
 (fn [db [_ identifier]]
   (get-in db [:results :summary-values identifier])))

(reg-sub
 :enrichment/max-p-value
 (fn [db]
   (get-in db [:results :enrichment-settings :maxp])))

(reg-sub
 :enrichment/test-correction
 (fn [db]
   (get-in db [:results :enrichment-settings :correction])))

(reg-sub
 :enrichment/background-population
 (fn [db]
   (get-in db [:results :enrichment-settings :population])))
