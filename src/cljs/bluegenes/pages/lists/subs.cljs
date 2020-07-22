(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :lists/root
 (fn [db]
   (:lists db)))

(reg-sub
 :lists/filtered-lists
 :<- [:lists]
 :<- [:current-mine-name]
 (fn [[all-lists current-mine-kw]]
   (get all-lists current-mine-kw)))
