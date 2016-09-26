(ns redgenes.components.querybuilder.events
  (:require
    [redgenes.components.querybuilder.core :refer [used-codes build-query next-code]]
    [com.rpl.specter :as s]
    [clojure.string :as string]
    [clojure.zip :as zip]
    #?(:clj  [clojure.core :refer [read-string]]
       :cljs [cljs.reader :refer [read-string]])))

(defn child-classes [c] (keyword (:referencedType c)))

(defn nth-child [z idx] (nth (iterate zip/right z) idx))

(defn reset-query
  "Returns the x for the given y"
  {:reframe-kind :event
   :reframe-key :query-builder/reset-query
   :undoable? true}
  [db [_ count]]
  (-> db
    (assoc-in [:query-builder :query] nil)
    (assoc-in [:query-builder :count] nil)
    (assoc-in [:query-builder :used-codes] nil)
    (assoc-in [:query-builder :where-tree] nil)))

(defn add-constraint-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx,
  :reframe-key   :query-builder/add-constraint
  :undoable?     true
  :undo-exp      "add constraint"}
  [{db :db} [_ constraint]]
  {:db       (let [used-codes
                   (last (sort (map :q/code
                                 (get-in
                                   db
                                   [:query-builder :query :q/where]))))
                   next-code (if (nil? used-codes)
                               "A"
                               (next-code used-codes))]
               (-> db
                 (update-in
                   [:query-builder :query :q/where]
                   (fn [where]
                     (conj (or where []) (merge constraint {:q/code next-code}))))
                 (assoc-in [:query-builder :constraint] nil))),
   :dispatch [:query-builder/run-query]})

(defn change-constraint-value
  "Returns the given db with the :q/where constraint value at given index
  changed to given value"
  {:reframe-kind :event
   :reframe-key  :query-builder/change-constraint-value
   :undoable?    true
   :undo-exp     (fn [db [_ index value]] (str "change constraint " index " to " value))}
  [db [_ index value]]
  (-> db
    (assoc-in [:query-builder :query :q/where index :q/value] value)))

(defn set-where-path
  ""
  {:reframe-kind :event, :reframe-key :query-builder/set-where-path}
  [db [_ path]]
  (-> db
    (assoc-in
      [:query-builder :query :path] path)))

(defn handle-count
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/handle-count}
  [db [_ count]]
  (-> db
    (assoc-in [:query-builder :count] count)
    (assoc-in [:query-builder :counting?] false)))

(defn run-query-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx, :reframe-key :query-builder/run-query}
  [{db :db}]
  (let [query-data (-> db :query-builder :query)]
    {:db                      (assoc-in db [:query-builder :counting?] true),
     :query-builder/run-query (build-query query-data)}))

(defn make-tree
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/make-tree}
  [db]
  (let [model (-> db :assets :model)] db))

(defn remove-select-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx, :reframe-key :query-builder/remove-select}
  [{db :db} [_ path]]
  {:db       (update-in
               db
               [:query-builder :query :q/select]
               (fn [views] (dissoc views path)))
   :dispatch :query-builder/run-query})

(defn remove-constraint-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/remove-constraint
   :undoable?    true
   :undo-exp     "remove constraint"}
  [{db :db} [_ path]]
  {:db       (update-in
               db
               [:query-builder :query :q/where]
               (fn [wheres] (remove #(= % path) wheres))),
   :dispatch [:query-builder/run-query]})

(defn add-filter
  "Returns the x for the given y"
  {:reframe-kind :event,
   :reframe-key :query-builder/add-filter
   :undoable? true}
  [db [_ path]]
  (assoc-in db [:query-builder :constraint] path))

(defn set-logic
  "Returns the x for the given y"
  {:reframe-kind :event
   :reframe-key :query-builder/set-logic
   :undoable? true}
  [db [_ expression]]
  (-> db
    (assoc-in [:query-builder :query :q/logic]
      (try
       (read-string (str "(" (string/upper-case expression) ")"))
       (catch #?(:clj Exception :cljs js/Error) e [])))
    (assoc-in [:query-builder :query :logic-str]
      expression)))

(defn set-query
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/set-query}
  [db [_ query-str]]
  (assoc-in
    db
    [:query-builder :query]
    (read-string query-str)))

(defn update-io-query
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/update-io-query}
  [db [_ query]]
  (assoc-in
    db
    [:query-builder :io-query]
    (build-query query)))

(defn add-view-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx
   :reframe-key :query-builder/add-view
   :undoable? true}
  [{db :db} [_ path-vec]]
  {:db       (update-in
               db
               [:query-builder :query :q/select]
               (fn [views]
                 (let [views (or views #{})]
                  (if (views path-vec)
                    (disj views path-vec)
                    (conj views path-vec)))))
   :dispatch [:query-builder/run-query]})
