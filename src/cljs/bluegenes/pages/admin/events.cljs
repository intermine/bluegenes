(ns bluegenes.pages.admin.events
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]
            [bluegenes.utils :refer [addvec remvec]]))

(def ^:const category-id-prefix "cat")
(def ^:const child-id-prefix "child")

(def root [:admin])

(reg-event-db
 ::init
 (fn [db [_]]
   (let [persisted-cats (get-in db [:mines (:current-mine db) :report-layout])]
     (-> db
         (update-in root dissoc :responses)
         (assoc-in (concat root [:categories]) persisted-cats)))))

(reg-event-db
 ::set-categorize-class
 (path root)
 (fn [admin [_ class-kw]]
   (assoc admin :categorize-class class-kw)))

(defn import-categories
  "Import persisted categories, adding newly generated IDs to categories and children."
  [cats]
  (into {}
        (map (fn [cat-kv]
               (update cat-kv 1
                       (partial mapv (fn [cat]
                                       (-> cat
                                           (assoc :id (gensym category-id-prefix))
                                           (update :children (partial mapv #(assoc % :id (gensym child-id-prefix))))))))))
        cats))

(defn export-categories
  "Export categories for persisting, removing IDs from categories and children."
  [cats]
  (into {}
        (map (fn [cat-kv]
               (update cat-kv 1
                       (partial mapv (fn [cat]
                                       (-> cat
                                           (dissoc :id)
                                           (update :children (partial mapv #(dissoc % :id)))))))))
        cats))

(reg-event-fx
 ::save-layout
 (fn [{db :db} [_ bg-properties-support?]]
   (let [categories (get-in db (concat root [:categories]))]
     (if bg-properties-support?
       {:dispatch [:property/save :layout.report (export-categories categories)
                   {:on-success [::save-layout-success]
                    :on-failure [::save-layout-failure]}]}
       {:db (assoc-in db [:mines (:current-mine db) :report-layout] categories)}))))

(reg-event-db
 ::save-layout-success
 (path root)
 (fn [admin [_]]
   (assoc-in admin [:responses :report-layout]
             {:type :success
              :message "Successfully saved changes to report page layout."})))

(reg-event-db
 ::save-layout-failure
 (path root)
 (fn [admin [_ res]]
   (assoc-in admin [:responses :report-layout]
             {:type :failure
              :message (str "Failed to save changes to report page layout. "
                            (not-empty (get-in res [:body :error])))})))

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
   :id (gensym category-id-prefix)
   :children []})

(defn new-child [child & {:keys [collapse]}]
  (assoc child
         :id (gensym child-id-prefix)
         :collapse collapse))

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
                (map new-child children))))

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

(reg-event-db
 ::child-set-description
 (path root)
 (fn [admin [_ cat-index child-index text]]
   (if (seq text)
     (to-child admin cat-index child-index :update
               assoc :description text)
     (to-child admin cat-index child-index :update
               dissoc :description))))
