(ns bluegenes.pages.admin.events
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]))

;; TODO when importing categories from webservice, make sure to add new IDs
;; TODO when exporting categories to webservice, make sure to remove all IDs
;; => use postwalk and assoc when map?

(def root [:admin])

(defn remvec
  [v i]
  (vec (concat (subvec v 0 i)
               (subvec v (inc i)))))

(defn addvec
  [v i e]
  (let [[before after] (split-at i v)]
    (vec (concat before [e] after))))

(reg-event-db
 ::set-categorize-class
 (path root)
 (fn [admin [_ class-kw]]
   (assoc admin :categorize-class class-kw)))

(reg-event-db
 ::category-add
 (path root)
 (fn [admin [_ category-name]]
   (-> admin
       (update :categories (fnil conj []) {:category category-name
                                           :id (gensym "cat")
                                           :children []})
       (update :new-category empty))))

(reg-event-db
 ::category-remove
 (path root)
 (fn [admin [_ cat-index]]
   (update admin :categories remvec cat-index)))

(reg-event-db
 ::category-move-up
 (path root)
 (fn [admin [_ cat-index]]
   (if (zero? cat-index)
     admin
     (let [categories (get admin :categories)
           cat (nth categories cat-index)]
       (assoc admin :categories
              (-> categories
                  (remvec cat-index)
                  (addvec (dec cat-index) cat)))))))

(reg-event-db
 ::category-move-down
 (path root)
 (fn [admin [_ cat-index]]
   (let [categories (get admin :categories)
         max-index (dec (count categories))]
     (if (>= cat-index max-index)
       admin
       (let [cat (nth categories cat-index)]
         (assoc admin :categories
                (-> categories
                    (remvec cat-index)
                    (addvec (inc cat-index) cat))))))))

(reg-event-db
 ::category-rename
 (path root)
 (fn [admin [_ cat-index new-name]]
   (update-in admin [:categories cat-index] assoc :category new-name)))

(reg-event-db
 ::set-new-category
 (path root)
 (fn [admin [_ new-category]]
   (assoc admin :new-category new-category)))
