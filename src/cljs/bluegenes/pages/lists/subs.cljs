(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.lists.utils :refer [normalize-lists]]))

(reg-sub
 :lists/root
 (fn [db]
   (:lists db)))

(reg-sub
 :lists/by-id
 :<- [:lists/root]
 (fn [root]
   (:by-id root)))

(reg-sub
 :lists/all-lists
 :<- [:lists/root]
 (fn [root]
   (vals (:by-id root))))

(reg-sub
 :lists/expanded-paths
 :<- [:lists/root]
 (fn [root]
   (:expanded-paths root)))

(reg-sub
 :lists/filtered-lists
 :<- [:lists/by-id]
 :<- [:lists/expanded-paths]
 (fn [[lists-by-id expanded-paths]]
   (normalize-lists identity identity {:by-id lists-by-id :expanded-paths expanded-paths})))
