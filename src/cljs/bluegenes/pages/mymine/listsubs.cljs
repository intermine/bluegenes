(ns bluegenes.pages.mymine.listsubs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :refer [upper-case]]
            [cljs-time.core :refer [before?]]
            [bluegenes.subs]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:title details)]
      (re-find (re-pattern (str "(?i)" string)) (clojure.string/join " " (map details [:title :description])))
      false)
    true))

(reg-sub
 :lists/text-filter
 (fn [db]
   (get-in db [:lists :controls :filters :text-filter])))

(reg-sub
 :lists/flag-filters
 (fn [db]
   (get-in db [:lists :controls :filters :flags])))

(reg-sub
 :lists/sort-order
 (fn [db]
   (get-in db [:lists :controls :sort])))

(defn tag-check?
  "Does this list contain (or not contain) a particular tag?"
  ([needle haystack]
   (tag-check? true needle haystack))
  ([has? needle haystack]
   (let [f (if has? some (complement some))]
     (some? (f #{needle} (:tags haystack))))))

(reg-sub
 :lists/filtered-lists
 :<- [:lists]
 :<- [:lists/text-filter]
 :<- [:lists/flag-filters]
 :<- [:lists/sort-order]
 :<- [:current-mine-name]
 (fn [[all-lists text-filter flag-filters sort-order current-mine-kw] _]
     ;; Apply any filters set by the user.
   (cond->> (get all-lists current-mine-kw)
     text-filter (filter (partial has-text? text-filter))
     (some? (:authorized flag-filters)) (filter #(= (:authorized %) (:authorized flag-filters)))
     (some? (:favourite flag-filters)) (filter (partial tag-check? (:favourite flag-filters) "im:favourite"))
       ;true (sort (apply comp (map build-comparer sort-order)))
     true (sort-by :timestamp >))))
