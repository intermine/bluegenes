(ns bluegenes.pages.admin.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::root
 (fn [db]
   (:admin db)))

(reg-sub
 ::categorize-class
 :<- [::root]
 (fn [admin]
   (:categorize-class admin)))

(reg-sub
 ::categories
 :<- [::root]
 (fn [admin]
   (:categories admin)))

(reg-sub
 ::new-category
 :<- [::root]
 (fn [admin]
   (:new-category admin)))
