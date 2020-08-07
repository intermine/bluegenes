(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.lists.utils :refer [normalize-lists ->filterf ->sortf filtered-list-ids-set]]
            [clojure.set :as set]))

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
 :lists/all-tags
 :<- [:lists/root]
 (fn [root]
   (:all-tags root)))

(reg-sub
 :lists/all-types
 :<- [:lists/root]
 (fn [root]
   (:all-types root)))

(reg-sub
 :lists/all-paths
 :<- [:lists/root]
 (fn [root]
   (:all-paths root)))

(reg-sub
 :lists/expanded-paths
 :<- [:lists/root]
 (fn [root]
   (:expanded-paths root)))

(reg-sub
 :lists/selected-lists
 :<- [:lists/root]
 (fn [root]
   (:selected-lists root)))

(reg-sub
 :lists/all-selected?
 :<- [:lists/filtered-list-ids-set]
 :<- [:lists/selected-lists]
 (fn [[list-ids selected-lists]]
   (if (empty? selected-lists)
     false
     ;; We use superset instead of equality as there may be more lists selected
     ;; under a different filter.
     (set/superset? selected-lists list-ids))))

(reg-sub
 :lists/pagination
 :<- [:lists/root]
 (fn [root]
   (:pagination root)))

(reg-sub
 :lists/per-page
 :<- [:lists/pagination]
 (fn [pagination]
   (:per-page pagination)))

(reg-sub
 :lists/current-page
 :<- [:lists/pagination]
 (fn [pagination]
   (:current-page pagination)))

(reg-sub
 :lists/controls
 :<- [:lists/root]
 (fn [root]
   (:controls root)))

(reg-sub
 :lists/filters
 :<- [:lists/controls]
 (fn [controls]
   (:filters controls)))

(reg-sub
 :lists/filter
 :<- [:lists/filters]
 (fn [filters [_ filter-name]]
   (get filters filter-name)))

(reg-sub
 :lists/keywords-filter
 :<- [:lists/filters]
 (fn [filters]
   (:keywords filters)))

(reg-sub
 :lists/sort
 :<- [:lists/controls]
 (fn [controls]
   (:sort controls)))

(reg-sub
 :lists/filtered-lists
 :<- [:lists/by-id]
 :<- [:lists/expanded-paths]
 :<- [:lists/filters]
 :<- [:lists/sort]
 :<- [:lists/pagination]
 (fn [[items-by-id expanded-paths active-filters active-sort pagination]]
   (normalize-lists
    (->filterf active-filters)
    (->sortf active-sort :folders-first? (= :folder (:lists active-filters)))
    {:by-id items-by-id :expanded-paths expanded-paths}
    pagination)))

(reg-sub
 :lists/no-filtered-lists?
 :<- [:lists/filtered-lists]
 (fn [lists]
   (empty? lists)))

;; Although normalize-lists is used here, similar to :lists/filtered-lists,
;; it differs in that all folders are expanded, the items are filtered but
;; not sorted, and we return a set of IDs. This will be the set of all list
;; IDs that are selectable with the currently active filters.
(reg-sub
 :lists/filtered-list-ids-set
 :<- [:lists/by-id]
 :<- [:lists/filters]
 (fn [[items-by-id active-filters]]
   (filtered-list-ids-set items-by-id active-filters)))

(reg-sub
 :lists/no-lists?
 :<- [:lists/by-id]
 (fn [items-by-id]
   (empty? items-by-id)))

(reg-sub
 :lists/top-level-count
 :<- [:lists/by-id]
 :<- [:lists/filters]
 (fn [[items-by-id active-filters]]
   (->> (normalize-lists
         (->filterf active-filters)
         identity
         {:by-id items-by-id :expanded-paths (constantly false)})
        (count))))

(reg-sub
 :lists/page-count
 :<- [:lists/per-page]
 :<- [:lists/top-level-count]
 (fn [[per-page total-count]]
   (if (pos? total-count)
     (Math/ceil (/ total-count per-page))
     0)))

(reg-sub
 :lists/modal-root
 :<- [:lists/root]
 (fn [root]
   (:modal root)))

(reg-sub
 :lists/active-modal
 :<- [:lists/modal-root]
 (fn [modal]
   (:active modal)))

(reg-sub
 :lists/modal-open?
 :<- [:lists/modal-root]
 (fn [modal]
   (:open? modal)))

;; If you have enough lists selected to perform a list operation.
(reg-sub
 :lists/selected-operation?
 :<- [:lists/selected-lists]
 (fn [lists]
   (> (count lists) 1)))

(reg-sub
 :lists/selected-lists-details
 :<- [:lists/by-id]
 :<- [:lists/selected-lists]
 (fn [[by-id selected-lists]]
   (map by-id selected-lists)))

(reg-sub
 :lists-modal/new-list-tags
 :<- [:lists/modal-root]
 (fn [modal]
   (:tags modal)))

(reg-sub
 :lists-modal/new-list-title
 :<- [:lists/modal-root]
 (fn [modal]
   (:title modal)))

(reg-sub
 :lists-modal/new-list-description
 :<- [:lists/modal-root]
 (fn [modal]
   (:description modal)))

(reg-sub
 :lists-modal/error
 :<- [:lists/modal-root]
 (fn [modal]
   (:error modal)))

(reg-sub
 :lists-modal/target-id
 :<- [:lists/modal-root]
 (fn [modal]
   (:target-id modal)))

(reg-sub
 :lists-modal/target-list
 :<- [:lists/by-id]
 :<- [:lists-modal/target-id]
 (fn [[by-id target-id]]
   (get by-id target-id)))

;; START COMMENT
;; These are only used for the subtract operation modal.
;; See `:lists/open-modal` event for where they are initialised.

(reg-sub
 :lists-modal/keep-lists
 :<- [:lists/modal-root]
 (fn [modal]
   (:keep-lists modal)))

(reg-sub
 :lists-modal/subtract-lists
 :<- [:lists/modal-root]
 (fn [modal]
   (:subtract-lists modal)))

(reg-sub
 :lists-modal/keep-lists-details
 :<- [:lists/by-id]
 :<- [:lists-modal/keep-lists]
 (fn [[by-id keep-lists]]
   (map by-id keep-lists)))

(reg-sub
 :lists-modal/subtract-lists-details
 :<- [:lists/by-id]
 :<- [:lists-modal/subtract-lists]
 (fn [[by-id subtract-lists]]
   (map by-id subtract-lists)))

;; END COMMENT

;; Expect a vector of strings.
(reg-sub
 :lists-modal/folder-path
 :<- [:lists/modal-root]
 (fn [modal]
   (:folder-path modal)))

;; Each path is a vector of strings representing the folder name at that level
;; of nesting. We can find suggestions by taking the existing paths that match
;; at all levels until the length of folder-path, then grab the next folder.
(reg-sub
 :lists-modal/folder-suggestions
 :<- [:lists/all-paths]
 :<- [:lists-modal/folder-path]
 (fn [[all-paths folder-path]]
   (let [folder-count (count folder-path)]
     (if (zero? folder-count)
       (->> all-paths (map first) distinct)
       (->> all-paths
            (remove (comp #(<= % folder-count) count))
            (filter (comp #(= % folder-path) #(subvec % 0 folder-count)))
            (map #(nth % folder-count))
            (distinct))))))
