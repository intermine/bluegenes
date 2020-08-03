(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.lists.utils :refer [normalize-lists internal-tag? folder?]]
            [clojure.string :as str])
  (:import goog.date.Date))

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

(defn ->filterf
  "Create a filter function from the filters map in controls, to be passed to
  `normalize-lists`. Remember that comp's arguments are run in reverse order!
  Will only be passed list maps, and not folders."
  [{:keys [keywords lists date type tags] :as _filterm}]
  (comp
   ;; Keyword filter should be done last.
   (if (empty? keywords)
     identity
     (let [keyws (map str/lower-case (-> keywords str/trim (str/split #"\s+")))]
       ;; Slightly faster; consider it if you wish to improve performance.
       #_(partial filter (fn [{:keys [title description]}]
                           (let [s (-> (str title " " description)
                                       (str/lower-case))]
                             (every? #(str/includes? s %) keyws))))
       ;; The following function filters by matching all the different fields
       ;; belonging to a list. Performance seems quite good even for 200 lists.
       (partial filter (fn [listm]
                         (let [all-text (->> listm
                                             ((juxt :title :size :description :type
                                                    ;; Note that internal tags aren't removed!
                                                    (comp #(str/join " " %) :tags)))
                                             (str/join " ")
                                             (str/lower-case))]
                           (every? #(str/includes? all-text %) keyws))))))
   ;; Filter by tag.
   (if (nil? tags)
     identity
     (partial filter (comp #(contains? % tags) set :tags)))
   ;; Filter by type.
   (if (nil? type)
     identity
     (partial filter (comp #{type} :type)))
   ;; Filter by details.
   (if (or (nil? lists) (= lists :folder)) ; Folders first handled in `->sortf`.
     identity
     (partial filter (case lists
                       :private (comp true? :authorized)
                       :public (comp false? :authorized))))
   ;; Filter by date.
   (if (nil? date)
     identity
     (let [now (.getTime (Date.))]
       (partial filter (case date
                         :day   (comp #(> (+ % 8.64e+7) now) :timestamp)
                         :week  (comp #(> (+ % 6.048e+8) now) :timestamp)
                         :month (comp #(> (+ % 2.628e+9) now) :timestamp)
                         :year  (comp #(> (+ % 3.154e+10) now) :timestamp)))))))

(defn ->sortf
  "Create a sort function from the sort map in controls, to be passed to
  `normalize-lists`. Remember that comp's arguments are run in reverse order!
  Will be passed both list and folder maps."
  [{:keys [column order] :as _sortm} & {:keys [folders-first?]}]
  (comp
   ;; Show private lists first.
   (partial sort-by :authorized (comp - compare))
   ;; Show folders first if the filter is applied.
   (if folders-first?
     (partial sort-by folder? (comp - compare))
     identity)
   ;; Sort according to active control.
   (partial sort-by
            ;; We don't want "B" to come before "a", so we lowercase strings.
            (comp #(cond-> % (string? %) str/lower-case)
                  ;; Filter away internal tags, which we don't care to sort after.
                  (if (= column :tags)
                    (partial filterv (complement internal-tag?))
                    identity)
                  column)
            ;; `compare` also works great for vectors, for which it will first
            ;; sort by length, then by each element.
            (case order
              :asc compare
              :desc (comp - compare)))))

(reg-sub
 :lists/filtered-lists
 :<- [:lists/by-id]
 :<- [:lists/expanded-paths]
 :<- [:lists/filters]
 :<- [:lists/sort]
 (fn [[lists-by-id expanded-paths active-filters active-sort]]
   (normalize-lists
    (->filterf active-filters)
    (->sortf active-sort :folders-first? (= :folder (:lists active-filters)))
    {:by-id lists-by-id :expanded-paths expanded-paths})))

(reg-sub
 :lists/no-filtered-lists?
 :<- [:lists/filtered-lists]
 (fn [lists]
   (empty? lists)))

(reg-sub
 :lists/no-lists?
 :<- [:lists/by-id]
 (fn [lists]
   (empty? lists)))

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
