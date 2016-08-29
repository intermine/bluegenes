(ns re-frame-boiler.sections.results.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :results/query
  (fn [db]
    (get-in db [:results :query])))