(ns bluegenes.pages.lists.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.lists.utils :refer [normalize-lists internal-tag? folder?]]
            [bluegenes.pages.lists.views :refer [parse-date-created]]
            [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time-coerce]))

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
       (partial filter (fn [{:keys [title description]}]
                         (let [s (-> (str title " " description)
                                     (str/lower-case))]
                           (every? #(str/includes? s %) keyws))))
       ;; The following function filters by matching all the different fields
       ;; belonging to a list. It's fancy, but sadly too slow for 50+ lists!
       #_(partial filter (fn [listm]
                           (let [all-text
                                 (str/lower-case
                                  (str/join
                                   " "
                                   ((juxt :title :size :description
                                          (comp #(parse-date-created % true) :dateCreated)
                                          :type (comp #(str/join " " %) :tags))
                                    listm)))]
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
     (let [now (time/now)]
       (partial filter (case date
                         :day   (comp #(time/after? % (time/minus now (time/days 1)))
                                      time-coerce/from-string :dateCreated)
                         :week  (comp #(time/after? % (time/minus now (time/weeks 1)))
                                      time-coerce/from-string :dateCreated)
                         :month (comp #(time/after? % (time/minus now (time/months 1)))
                                      time-coerce/from-string :dateCreated)
                         :year  (comp #(time/after? % (time/minus now (time/years 1)))
                                       time-coerce/from-string :dateCreated)))))))

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
