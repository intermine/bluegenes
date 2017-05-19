(ns bluegenes.components.querybuilder.events
  "
  All the things that can change
  the state of the db via the query builder

  Functions here should be testable in the repl
  cross-platform, and Specable at some point.

  They have metadata for reframe registration
  and sometimes an extra 3 arg implementation
  which returns an undo explanation map/string
  (not ideal, but i wanted a way to see a fn
  and its undo-string fn together and since metadata
  is static in cljs this was a quick way to have that)
  (if the reframe undo/redo explanation needs to be
  dynamic, then the :undo-exp key is a keyword, indicating
  that the 3-arg form of the fn is to be used for an undo
  explanation, otherwise it's a string explanation of the undo)



  There's two representation of the query:

  the query structure being validated & modified by the user
  is one thing, a convenient structure for changing & checking
  with Spec. The one being sent to the server is built
  whenever we want to do IO & use the webservice


  todo:

  * when a constraint is removed from where, sometimes if the associated
    attribute is still in the select, it affects the results & can return
    zero results -- maybe automatically remove from select if no where
    clauses using it ?

  "
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require
    [bluegenes.components.querybuilder.core :as c :refer
      [build-query next-code to-list]]
    #?(:cljs [re-frame.core :as re-frame :refer [dispatch subscribe]])
    #?(:cljs [cljs.spec :as spec] :clj [clojure.spec :as spec])
    #?(:cljs [cljs.core.async :refer [put! chan <! >! timeout close!]])
    #?(:cljs [imcljsold.search :as search])
    #?(:cljs [imcljsold.filters :as filters])
    [bluegenes.utils :refer [register-all!]]
    [com.rpl.specter :as s]
    [clojure.string :as string]
    [clojure.zip :as zip]))

#_(def im-zipper (zip/zipper
                   (fn branch? [node] true)
                   (fn children [node]
                     (println "raw" (:collections node))
                     (let [child-classes (map (comp child-classes second) (:collections node))]
                       (.log js/console "returning" (select-keys model child-classes))
                       (select-keys model child-classes)))
                   (fn make-node [node children]
                     (println "makde node called")
                     (assoc node :collections children))
                   (-> db :assets :model :Gene)))

(defn child-classes [c] (keyword (:referencedType c)))

(defn nth-child [z idx] (nth (iterate zip/right z) idx))

(defn parse-long [s]
  #?(:cljs (.parseFloat js/Number s)
     :clj  (Long/parseLong s)))

(defn reset-query
  "Reset the query to empty"
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

(defn update-io-query
  "Updates the Webservice query
  from the given Spec query"
  {:reframe-kind :event, :reframe-key :query-builder/update-io-query}
  [{{query :query} :query-builder :as db} _]
  ;(println "ioq" query (spec/valid? :q/query query) (get-in db [:query-builder :io-query]))
  (if (spec/valid? :q/query query)
    (assoc-in
      db
      [:query-builder :io-query]
      (build-query query))
    db))

(defn add-constraint
  "Add a where clause"
  {:reframe-kind :cofx,
   :reframe-key   :query-builder/add-constraint
   :undoable?     true
   :undo-exp      "add constraint"}
  ([db [_ constraint]]
    (let [used-codes
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
       (assoc-in [:query-builder :constraint] nil)))))

(defn add-constraint-cofx
  "Adds a where clause, runs the query"
  {:reframe-kind :cofx,
   :reframe-key   :query-builder/add-constraint!
   :undoable?     true
   :undo-exp      "add constraint"}
  [{db :db} event]
  {:db       (update-io-query (add-constraint db event) event)
   :dispatch [:query-builder/run-query!]})


(defn change-constraint-value
  "Returns the given db with the :q/where constraint value at given index
  changed to given value"
  {:reframe-kind :event
   :reframe-key  :query-builder/change-constraint-value
   :undoable?    true
   :undo-exp     :use-this-fn-due-to-static-metadata-in-cljs}
  ([_ db [_ index value]]
   {:explanation (str "change constraint value to " value)
    :count (get-in db [:query-builder :count])
    :dcount (get-in db [:query-builder :dcount])})
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
   {:explanation (str "change constraint op to " op)
    :count (get-in db [:query-builder :count])
    :dcount (get-in db [:query-builder :dcount])})
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
  "Adds the count & difference in count (dcount)"
  {:reframe-kind :event, :reframe-key :query-builder/handle-count}
  [db [_ count]]
  (let [
          count (try (parse-long count) (catch #?(:cljs js/Error :clj Exception) e e))
          pcount (get-in db [:query-builder :count])
          dc (if (and (number? count) (number? pcount)) (- count pcount) 0)
        ]
    (-> db
     (assoc-in [:query-builder :count] count)
     (assoc-in [:query-builder :dcount] dc)
     (assoc-in [:query-builder :counting?] false))))

(defn run-query-cofx
  "Returns a cofx for running the query"
  {:reframe-kind :cofx, :reframe-key :query-builder/run-query!}
  [{db :db}]
  (let [query-data (-> db :query-builder :query)]
    {:db (assoc-in db [:query-builder :counting?] true),
     :query-builder/maybe-run-query!
         {:query  (get-in db [:query-builder :query])
          :query? (get-in db [:query-builder :autoupdate?])}}))

(defn maybe-run-query-cofx
  "Returns a cofx for maybe running the query"
  {:reframe-kind :cofx, :reframe-key :query-builder/maybe-run-query}
  [{db :db}]
  {:db (update-io-query db 7)
   :query-builder/maybe-run-query!
       {:query  (build-query (get-in db [:query-builder :query]))
        :query? (get-in db [:query-builder :autoupdate?])}})

(defn make-tree
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/make-tree}
  [db]
  (let [model (-> db :assets :model)] db))

(defn remove-select-cofx
  "Removes the given path from the select clause"
  {:reframe-kind :cofx, :reframe-key :query-builder/remove-select}
  [{db :db} [_ path]]
  {:db       (update-in
               db
               [:query-builder :query :q/select]
               (fn [views] (dissoc views path)))
   :dispatch :query-builder/maybe-run-query})

(defn update-paths
  ([db]
    (assoc-in db [:query-builder :query :constraint-paths]
      (apply hash-set (map :q/path (get-in db [:query-builder :query :q/where]))))))

(defn remove-constraint-cofx
  "Removes the given path from select, runs the query"
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/remove-constraint
   :undoable?    true
   :undo-exp     "remove constraint"}
  [{db :db} [_ c i]]
  {:db
             (update-paths
               (update-in db
                [:query-builder :query :q/where]
                (fn [wheres] (vec (remove #(= % c) wheres)))))
   :dispatch [:query-builder/maybe-run-query]})

(defn add-filter
  "Returns the x for the given y"
  {:reframe-kind :event,
   :reframe-key :query-builder/add-filter
   :undoable? true}
  [db [_ path typ]]
  (assoc-in db [:query-builder :constraint] {:path path :typ typ}))

(defn set-logic
  "Parse the given logic expression to a list"
  {:reframe-kind :event
   :reframe-key :query-builder/set-logic
   :undoable? true
   :undo-exp :qwe}
  ([_ db [_ expression]]
   {
    :dcount (get-in db [:query-builder :dcount])
    :count (get-in db [:query-builder :count])
    :explanation "set the logic"})
  ([db [_ expression]]
   (let [x (try
             (c/simplify (c/infix-prefix (c/group-ands (c/to-list (str "(" expression ")")))))
             (catch #?(:clj Exception :cljs js/Error) e []))]
     (-> db
       (assoc-in [:query-builder :query :q/logic] x)
       (assoc-in [:query-builder :query :logic-exp] (c/prefix-infix x))
       (assoc-in [:query-builder :query :logic-str]
         (string/upper-case expression))))))
       ;(str (c/prefix-infix x))

(defn set-query
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/set-query}
  [db [_ query-str]]
  (assoc-in
    db
    [:query-builder :query]
    (to-list query-str)))

(defn set-logic-cofx
  "Sets the query constraint logic"
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/set-logic!
   :undoable?    true
   :undo-exp     :see-also}
  ([_ {db :db} event]
    {:count (get-in db [:query-builder :count]) :explanation "set logic!"})
  ([{db :db} event]
   {:db       (let [db (set-logic db event)]
                db
                (if (get-in db [:query-builder :autoupdate?])
                  (update-io-query db [nil (get-in db [:query-builder :query])])
                  db))
    :dispatch [:query-builder/maybe-run-query]}))

(defn toggle-view
  "Toggles the given path in the querie's select"
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
  "Toggles a select clause, runs the query"
  {:reframe-kind :cofx
   :reframe-key :query-builder/toggle-view!
   :undoable? true}
  [state event]
  {:db (toggle-view state event)
   :dispatch [:query-builder/run-query!]})


(defn run-query!
   "Runs the given query, returns a channel"
  {:reframe-kind :fx, :reframe-key :query-builder/run-query!}
   [query]

        #?(:cljs
                (go
                  (dispatch
                    [:query-builder/handle-count
                     (<!
                       (search/raw-query-rows
                         (select-keys (:service @(subscribe [:current-mine])) [:root ])
                         query
                         {:format "count"}))]))
           :clj (println "run query")))

(defn maybe-run-query!
   "Maybe runs the given query, if it
   conforms to the exacting requirements of spec"
   {:reframe-kind :fx, :reframe-key :query-builder/maybe-run-query!}
   [{query :query query? :query?}]
        #?(:cljs
                (cond
                  (spec/valid? :q/query query)
                  (run-query! (build-query query)))
           :clj (println "maybe run query")))

(def my-events
  [
   #'bluegenes.components.querybuilder.events/reset-query
   #'bluegenes.components.querybuilder.events/add-constraint-cofx
   #'bluegenes.components.querybuilder.events/change-constraint-value
   #'bluegenes.components.querybuilder.events/change-constraint-op
   #'bluegenes.components.querybuilder.events/handle-count
   #'bluegenes.components.querybuilder.events/run-query-cofx
   #'bluegenes.components.querybuilder.events/make-tree
   #'bluegenes.components.querybuilder.events/remove-select-cofx
   #'bluegenes.components.querybuilder.events/remove-constraint-cofx
   #'bluegenes.components.querybuilder.events/add-filter
   #'bluegenes.components.querybuilder.events/set-logic
   #'bluegenes.components.querybuilder.events/set-logic-cofx
   #'bluegenes.components.querybuilder.events/set-query
   #'bluegenes.components.querybuilder.events/update-io-query
   #'bluegenes.components.querybuilder.events/toggle-view-cofx
   #'bluegenes.components.querybuilder.events/maybe-run-query-cofx
   #'bluegenes.components.querybuilder.events/set-where-path
   #'bluegenes.components.querybuilder.events/run-query!
   #'bluegenes.components.querybuilder.events/maybe-run-query!])

#?(:cljs (register-all! my-events))
