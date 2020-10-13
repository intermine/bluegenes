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
