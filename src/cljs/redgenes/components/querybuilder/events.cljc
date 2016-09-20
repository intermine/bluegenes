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
  {:reframe-kind :event, :reframe-key :query-builder/reset-query}
  [db [_ count]]
  (-> db
    (assoc-in [:query-builder :query] nil)
    (assoc-in [:query-builder :count] nil)
    (assoc-in [:query-builder :used-codes] nil)))

(defn add-constraint-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx, :reframe-key :query-builder/add-constraint}
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
                     (conj where (merge constraint {:q/code next-code}))))
                 (assoc-in [:query-builder :constraint] nil))),
   :dispatch [:query-builder/run-query]})

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
               (fn [views] (remove #(= % path) views)))
   :dispatch :query-builder/run-query})

(defn remove-constraint-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx,
   :reframe-key  :query-builder/remove-constraint}
  [{db :db} [_ path]]
  {:db       (update-in
               db
               [:query-builder :query :q/where]
               (fn [wheres] (remove #(= % path) wheres))),
   :dispatch [:query-builder/run-query]})

(defn add-filter
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/add-filter}
  [db [_ path]]
  (assoc-in db [:query-builder :constraint] path))

(defn set-logic
  "Returns the x for the given y"
  {:reframe-kind :event, :reframe-key :query-builder/set-logic}
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

(defn add-view-cofx
  "Returns the x for the given y"
  {:reframe-kind :cofx, :reframe-key :query-builder/add-view}
  [{db :db} [_ path-vec]]
  {:db       (update-in
               db
               [:query-builder :query :q/select]
               (fn [views]
                 (if (some #(= % path-vec) views)
                   (remove #(= % path-vec) views)
                   (conj views path-vec)))),
   :dispatch [:query-builder/run-query]})
