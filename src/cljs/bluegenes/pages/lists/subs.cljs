(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.lists.utils :refer [normalize-lists]]
            [bluegenes.pages.lists.views :refer [parse-date-created]]
            [clojure.string :as str]))

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
 :lists/expanded-paths
 :<- [:lists/root]
 (fn [root]
   (:expanded-paths root)))

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
  [{:keys [keywords] :as _filterm}]
  (if (empty? keywords)
    identity
    (let [keyws (map str/lower-case (-> keywords str/trim (str/split #"\s+")))]
      (partial filter (fn [listm]
                        (let [all-text
                              (str/join
                               " "
                               ((juxt :title :size :description
                                      (comp #(parse-date-created % true) :dateCreated)
                                      :type (comp #(str/join " " %) :tags))
                                listm))]
                          (if-let [s (-> all-text str/lower-case not-empty)]
                            (every? #(str/includes? s %) keyws)
                            false)))))))

(defn ->sortf
  "Create a sort function from the sort map in controls, to be passed to
  `normalize-lists`. Remember that comp's arguments are run in reverse order!
  Will be passed both list and folder maps."
  [{:keys [column order] :as _sortm}]
  (comp
   ;; Show private lists first.
   (partial sort-by :authorized (comp - compare))
   ;; Sort according to active control.
   (partial sort-by
            ;; We don't want "B" to come before "a", so we lowercase strings.
            (comp #(cond-> % (string? %) str/lower-case) column)
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
    (->sortf active-sort)
    {:by-id lists-by-id :expanded-paths expanded-paths})))
