(ns redgenes.components.querybuilder.actions
  (:require
    #?(:cljs [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]])
    [com.rpl.specter :as s]
    [clojure.zip :as zip]))

(defn child-classes [c]
  (keyword (:referencedType c)))

(defn nth-child [z idx]
  (nth (iterate zip/right z) idx))

(defn next-letter [letter]
  (let [alphabet (into [] "ABCDEFGHIJKLMNOPQRSTUVWXYZ")]
    (first (rest (drop-while (fn [n] (not= n letter)) alphabet)))))

(defn
  ^{:reframe-key :query-builder/reset-query
    :reframe-kind :event}
  reset-query
  "Returns the given app state
  with the query reset"
  [db [_ count]]
  (println "reset query!!")
  (-> db
    (assoc-in [:query-builder :query] nil)
    (assoc-in [:query-builder :count] nil)))

(defn do-reg!
  ([v]
   #?(:cljs
    (do-reg! @v (meta v))))
  ([f {:keys [reframe-key reframe-kind]}]
   #?(:cljs
      (case reframe-kind
        :effect (reg-event-db reframe-key f)
        :fx     (reg-fx reframe-key f)
        :cofx   (reg-event-fx reframe-key f)))))

(comment
  (reg-event-fx
   :query-builder/add-constraint
   (fn [{db :db} [_ constraint]]
     {:db       (let [used-codes (last (sort (map :code (get-in db [:query-builder :query :where]))))
                      next-code (if (nil? used-codes) "A" (next-letter used-codes))]
                  (-> db
                    (update-in [:query-builder :query :where] (fn [where] (conj where (merge constraint {:code next-code}))))
                    (assoc-in [:query-builder :constraint] nil)))
      :dispatch [:query-builder/run-query]}))

  (reg-event-db
    :query-builder/handle-count
    (fn [db [_ count]]
      (-> db
        (assoc-in [:query-builder :count] count)
        (assoc-in [:query-builder :counting?] false))))

  (reg-fx
    :query-builder/run-query
    (fn [query]
      (go (dispatch [:query-builder/handle-count (<! (search/raw-query-rows
                                                       {:root @(subscribe [:mine-url])}
                                                       query
                                                       {:format "count"}))]))))

  (reg-event-fx
    :query-builder/run-query
    (fn [{db :db}]
      (let [query-data (-> db :query-builder :query)]
        {:db                      (assoc-in db [:query-builder :counting?] true)
         :query-builder/run-query (-> query-data
                                    (update :select (fn [views] (map (fn [view] (clojure.string/join "." view)) views)))
                                    (update :where (fn [cons]
                                                     (map (fn [con]
                                                            {:path  (clojure.string/join "." (:path con))
                                                             :op    (:op con)
                                                             :value (:value con)}) cons))))})))

  (reg-event-db
    :query-builder/make-tree
    (fn [db]
      (let [model (-> db :assets :model)]
        #_(assoc-in db [:query-builder :query]
            {:from   "Gene"
             :select [["Gene" "alleles" "alleleClass"]
                      ["Gene" "secondaryIdentifier"]
                      ["Gene" "primaryIdentifier"]]})
        db)))

  (reg-event-fx
    :query-builder/remove-select
    (fn [{db :db} [_ path]]
      {:db       (update-in db [:query-builder :query :select]
                   (fn [views]
                     (remove #(= % path) views)))
       :dispatch :query-builder/run-query}))

  (reg-event-fx
    :query-builder/remove-constraint
    (fn [{db :db} [_ path]]
      {:db       (update-in db [:query-builder :query :where]
                   (fn [wheres]
                     (remove #(= % path) wheres)))
       :dispatch [:query-builder/run-query]}))

  (reg-event-db
    :query-builder/add-filter
    (fn [db [_ path]]
      (assoc-in db [:query-builder :constraint] path)))

  (reg-event-fx
    :query-builder/add-view
    (fn [{db :db} [_ path-vec]]
      {:db       (update-in db [:query-builder :query :select]
                   (fn [views]
                     (if (some #(= % path-vec) views)
                       (remove #(= % path-vec) views)
                       (conj views path-vec))))
       :dispatch [:query-builder/run-query]})))

(do-reg!
  (comp reset-query (fn [db _] (println "something first") db))
  :query-builder/reset-query)
