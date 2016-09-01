(ns redgenes.sections.saveddata.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :saved-data/all
  (fn [db]
    (sort-by (fn [[_ {created :created}]] created) > (get-in db [:saved-data :items]))))

(reg-sub
  :saved-data/edit-mode
  (fn [db]
    (get-in db [:saved-data :list-operations-enabled])))

(reg-sub
  :saved-data/section
  (fn [db]
    (:saved-data db)))