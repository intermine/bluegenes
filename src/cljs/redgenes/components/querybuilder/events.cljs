(ns redgenes.components.querybuilder.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.filters :as filters]
            [com.rpl.specter :as s]
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

(defn child-classes [c]
  (keyword (:referencedType c)))

(defn nth-child [z idx]
  (nth (iterate zip/right z) idx))

(reg-event-db
  :query-builder/reset-query
  (fn [db [_ count]]
    (-> db
      (assoc-in [:query-builder :query] nil)
      (assoc-in [:query-builder :count] nil))))

(defn next-letter [letter]
  (let [alphabet (into [] "ABCDEFGHIJKLMNOPQRSTUVWXYZ")]
    (first (rest (drop-while (fn [n] (not= n letter)) alphabet)))))

(reg-event-fx
  :query-builder/add-constraint
  (fn [{db :db} [_ constraint]]
    {:db       (let [used-codes (last (sort (map :code (get-in db [:query-builder :query :where]))))
                     next-code  (if (nil? used-codes) "A" (next-letter used-codes))]
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
      {:db        (assoc-in db [:query-builder :counting?] true)
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
     :dispatch [:query-builder/run-query]}))
