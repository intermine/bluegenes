(ns bluegenes.pages.home.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :home/root
 (fn [db]
   (:home db)))

(reg-sub
 :home/active-template-category
 :<- [:home/root]
 (fn [home]
   (:active-template-category home)))
