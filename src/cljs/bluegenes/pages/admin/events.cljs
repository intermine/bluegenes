(ns bluegenes.pages.admin.events
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]
            [bluegenes.utils :refer [addvec remvec]]))

;; TODO when importing categories from webservice, make sure to add new IDs
;; TODO when exporting categories to webservice, make sure to remove all IDs
;; => use postwalk and assoc when map?

(def root [:admin])

(reg-event-db
 ::set-categorize-class
 (path root)
 (fn [admin [_ class-kw]]
   (assoc admin :categorize-class class-kw)))

(defn get-categorize-class [admin]
  (or (get admin :categorize-class)
      ;; This is to replace `nil` when the dropdown is set to Default.  It's a
      ;; keyword instead of string so it won't conflict if there happens to be
      ;; a real class called Default, and lower case to distinguish it further.
      :default))

;; We don't want to repeatedly encode the structure of categories into all our
;; events, so we create utility functions to make things more stratified.

(defn to-categories-path [path admin in-kw & args]
  (let [target-class (get-categorize-class admin)
        in-f (case in-kw
               :get get-in
               :update update-in
               :assoc assoc-in)]
    (apply in-f admin (concat [:categories target-class] path) args)))

(defn to-categories [admin in-kw & args]
  (apply to-categories-path [] admin in-kw args))

(defn to-category [admin cat-index in-kw & args]
  (apply to-categories-path [cat-index] admin in-kw args))

(defn to-children [admin cat-index in-kw & args]
  (apply to-categories-path [cat-index :children] admin in-kw args))

(defn to-child [admin cat-index child-index in-kw & args]
  (apply to-categories-path [cat-index :children child-index] admin in-kw args))

;; You can think of the above functions as magically being replaced with the
;; correct invocation of get-in, assoc-in or update-in, with the remaining
;; arguments (args) appended to the end.

(defn new-category [cat-name]
  {:category cat-name
   :id (gensym "cat")
   :children []})

(reg-event-db
 ::category-add
 (path root)
 (fn [admin [_ category-name]]
   (-> admin
       (to-categories :update (fnil conj []) (new-category category-name))
       (update :new-category empty))))

(reg-event-db
 ::category-remove
 (path root)
 (fn [admin [_ cat-index]]
   (to-categories admin :update remvec cat-index)))

(reg-event-db
 ::category-move-up
 (path root)
 (fn [admin [_ cat-index]]
   (if (zero? cat-index)
     admin
     (let [categories (to-categories admin :get)
           cat (nth categories cat-index)]
       (to-categories admin :assoc
                      (-> categories
                          (remvec cat-index)
                          (addvec (dec cat-index) cat)))))))

(reg-event-db
 ::category-move-down
 (path root)
 (fn [admin [_ cat-index]]
   (let [categories (to-categories admin :get)
         last-index (dec (count categories))]
     (if (>= cat-index last-index)
       admin
       (let [cat (nth categories cat-index)]
         (to-categories admin :assoc
                        (-> categories
                            (remvec cat-index)
                            (addvec (inc cat-index) cat))))))))

(reg-event-db
 ::category-rename
 (path root)
 (fn [admin [_ cat-index new-name]]
   (to-category admin cat-index :update assoc :category new-name)))

(reg-event-db
 ::set-new-category
 (path root)
 (fn [admin [_ new-category]]
   (assoc admin :new-category new-category)))

(reg-event-db
 ::children-add
 (path root)
 (fn [admin [_ cat-index children]]
   (to-children admin cat-index :update
                (fnil into [])
                (map #(assoc % :id (gensym "child")) children))))

(reg-event-db
 ::child-remove
 (path root)
 (fn [admin [_ cat-index child-index]]
   (to-children admin cat-index :update
                remvec child-index)))

(reg-event-db
 ::child-move-up
 (path root)
 (fn [admin [_ cat-index child-index]]
   (if (zero? child-index)
     admin
     (let [children (to-children admin cat-index :get)
           child (nth children child-index)]
       (to-children admin cat-index :assoc
                    (-> children
                        (remvec child-index)
                        (addvec (dec child-index) child)))))))

(reg-event-db
 ::child-move-down
 (path root)
 (fn [admin [_ cat-index child-index]]
   (let [children (to-children admin cat-index :get)
         last-index (dec (count children))]
     (if (>= cat-index last-index)
       admin
       (let [child (nth children child-index)]
         (to-children admin cat-index :assoc
                      (-> children
                          (remvec child-index)
                          (addvec (inc child-index) child))))))))

(reg-event-db
 ::child-set-collapse
 (path root)
 (fn [admin [_ cat-index child-index state]]
   (to-child admin cat-index child-index :update
             assoc :collapse state)))
