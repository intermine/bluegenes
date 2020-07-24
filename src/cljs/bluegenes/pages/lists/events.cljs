(ns bluegenes.pages.lists.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]
            [bluegenes.pages.lists.utils :refer [denormalize-lists]]))

(def root [:lists])

;; A hash-map is more amenable to locating specific lists, so we copy the
;; vector of lists into a id->list map.
(reg-event-db
 :lists/initialize
 (fn [db]
   (let [all-lists (get-in db [:assets :lists (:current-mine db)])]
     (assoc-in db (conj root :by-id) (denormalize-lists all-lists)))))

(reg-event-db
 :lists/expand-path
 (path root)
 (fn [lists [_ path]]
   (update lists :expanded-paths (fnil conj #{}) path)))

(reg-event-db
 :lists/collapse-path
 (path root)
 (fn [lists [_ path]]
   (update lists :expanded-paths (fnil disj #{}) path)))
