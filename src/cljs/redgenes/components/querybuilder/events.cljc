(ns redgenes.components.querybuilder.events
  (:require
    [redgenes.components.querybuilder.core :as c :refer
      [used-codes build-query next-code to-list]]
    [com.rpl.specter :as s]
    [clojure.string :as string]
    [clojure.zip :as zip]))

(defn child-classes [c] (keyword (:referencedType c)))

(defn nth-child [z idx] (nth (iterate zip/right z) idx))

(defn reset-query
  "Returns the x for the given y"
  {:reframe-kind :event
   :reframe-key :query-builder/reset-query
   :undoable? true}
  [db [_ count]]
  (-> db
    (assoc-in [:query-builder :query] {
                                       :q/select #{}
                                       :q/where []
                                       :constraint-paths #{}})

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
                 (update-in [:query-builder :query :constraint-paths]
                   (fn [cs] (conj (or cs #{}) (:q/path constraint))))
                 (update-in
                   [:query-builder :query :q/where]
                   (fn [where]
                     (conj (or where []) (merge constraint {:q/code next-code}))))
                 (assoc-in [:query-builder :constraint] nil))),
   :dispatch [:query-builder/run-query!]})

(defn change-constraint-value
  "Returns the given db with the :q/where constraint value at given index
  changed to given value"
  {:reframe-kind :event
   :reframe-key  :query-builder/change-constraint-value
   :undoable?    true
   :undo-exp     :use-this-fn-due-to-static-metadata-in-cljs}
  ([_ db [_ index value]]
   (str "change constraint value to " value))
  ([db [_ index value]]
   (-> db
     (assoc-in [:query-builder :query :q/where index :q/value] value))))

(defn change-constraint-op
  "Returns the given db with the :q/where constraint op at given index
  changed to given value"
  {:reframe-kind :event
   :reframe-key  :query-builder/change-constraint-op
   :undoable?    true
   :undo-exp     :use-this-fn-due-to-static-metadata-in-cljs}
  ([_ db [_ index op]]
   (str "change constraint operation to " op))
  ([db [_ index op]]
   (-> db
     (assoc-in [:query-builder :query :q/where index :q/op] op))))

(defn set-where-path
  ""
  {:reframe-kind :event, :reframe-key :query-builder/set-where-path}
  [db [_ path]]
  (-> db
    (assoc-in
      [:query-builder :query :path] path)))

(defn handle-count
  "Adds the count"
  {:reframe-kind :event, :reframe-key :query-builder/handle-count}
  [db [_ count]]
  (-> db
    (assoc-in [:query-builder :count] count)
    (assoc-in [:query-builder :counting?] false)))

(defn toggle-autoupdate
  "Toggle autoupdate"
  {:reframe-kind :event, :reframe-key :query-builder/toggle-autoupdate}
  [db [_ count]]
  (update-in db [:query-builder :autoupdate?] not))

(defn run-query-cofx
  "Returns a cofx for running the query"
  {:reframe-kind :cofx, :reframe-key :query-builder/run-query!}
  [{db :db}]
  (let [query-data (-> db :query-builder :query)]
    {:db                       (assoc-in db [:query-builder :counting?] true),
     :query-builder/run-query! (build-query query-data)}))

(defn maybe-run-query-cofx
  "Returns a cofx for maybe running the query"
  {:reframe-kind :cofx, :reframe-key :query-builder/maybe-run-query}
  [{db :db}]
  {:db                       db
   :query-builder/maybe-run-query!
     {:query  (build-query (get-in db [:query-builder :query]))
      :query? (get-in db [:query-builder :autoupdate?])}})

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
   :dispatch :query-builder/maybe-run-query})

(defn remove-constraint-cofx
  "Returns "
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/remove-constraint
   :undoable?    true
   :undo-exp     "remove constraint"}
  [{db :db} [_ path i]]
  {:db
     (update-in
       db
       [:query-builder :query :q/where]
       (fn [wheres] (vec (remove #(= % path) wheres))))
   :dispatch [:query-builder/maybe-run-query]})

(defn add-filter
  "Returns the x for the given y"
  {:reframe-kind :event,
   :reframe-key :query-builder/add-filter
   :undoable? true}
  [db [_ path]]
  (assoc-in db [:query-builder :constraint] path))

(defn set-logic
  "Parse the given logic expression to a list"
  {:reframe-kind :event
   :reframe-key :query-builder/set-logic
   :undoable? true}
  [db [_ expression]]
  (let [x (try
            (c/simplify (c/to-prefix (c/group-ands (c/to-list (str "(" expression ")")))))
            (catch #?(:clj Exception :cljs js/Error) e []))]
    (-> db
     (assoc-in [:query-builder :query :q/logic] x)
     (assoc-in [:query-builder :query :logic-exp] (c/prefix-infix x))
     (assoc-in [:query-builder :query :logic-str]
       (string/upper-case expression)))))
       ;(str (c/prefix-infix x))


(defn set-query
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/set-query}
  [db [_ query-str]]
  (assoc-in
    db
    [:query-builder :query]
    (to-list query-str)))

(defn update-io-query
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/update-io-query}
  [db [_ query]]
  (assoc-in
    db
    [:query-builder :io-query]
    (build-query query)))

(defn set-logic-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/set-logic!
   :undoable?    true
   :undo-exp     "set logic"}
  [{db :db} event]
  {:db       (let [db (set-logic db event)]
               db
               (if (get-in db [:query-builder :autoupdate?])
                 (update-io-query db [nil (get-in db [:query-builder :query])])
                 db))
   :dispatch [:query-builder/maybe-run-query]})

(defn toggle-view
  "Returns the x for the given y"
  {:reframe-kind :event
   :reframe-key :query-builder/toggle-view
   :undoable? true}
  [{db :db} [_ path-vec]]
  (update-in
    db
    [:query-builder :query :q/select]
    (fn [views]
      (let [views (or views #{})]
        (if (views path-vec)
          (disj views path-vec)
          (conj views path-vec))))))

(defn toggle-view-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx
   :reframe-key :query-builder/toggle-view!
   :undoable? true}
  [state event]
  {:db (toggle-view state event)
   :dispatch [:query-builder/run-query!]})
