(ns bluegenes.pages.lists.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]
            [bluegenes.pages.lists.utils :refer [denormalize-lists path-prefix?]]))

;; TODO make sure :lists/initialize is run when lists assets update
;; - check if active-panel is lists page, then dispatch it

(def root [:lists])

;; A hash-map is more amenable to locating specific lists, so we copy the
;; vector of lists into a id->list map.
(reg-event-db
 :lists/initialize
 (fn [db]
   (let [all-lists (get-in db [:assets :lists (:current-mine db)])]
     (update-in db root assoc
                :by-id (denormalize-lists all-lists)
                :all-tags (->> all-lists (mapcat :tags) distinct (remove path-prefix?) sort)
                :all-types (->> all-lists (map :type) distinct sort)))))

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

;; Note: Do not dispatch this from more than one place.
;; The input field which changes this value uses debouncing and internal state,
;; so it won't sync with this value except when first mounting.
(reg-event-db
 :lists/set-keywords-filter
 (path root)
 (fn [lists [_ keywords-string]]
   (assoc-in lists [:controls :filters :keywords] keywords-string)))

(reg-event-db
 :lists/toggle-sort
 (path root)
 (fn [lists [_ column]]
   (update-in lists [:controls :sort]
              (fn [{old-column :column old-order :order}]
                {:column column
                 :order (if (= old-column column)
                          (case old-order
                            :asc :desc
                            :desc :asc)
                          :asc)}))))

(reg-event-db
 :lists/set-filter
 (path root)
 (fn [lists [_ filter-name value]]
   (assoc-in lists [:controls :filters filter-name] value)))