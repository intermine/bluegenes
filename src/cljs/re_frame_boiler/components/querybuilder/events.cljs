(ns re-frame-boiler.components.querybuilder.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
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

(reg-event
  :handle-count
  (fn [db [_ count]]
    (assoc-in db [:query-builder :count] count)))

(reg-event
  :qb-reset-query
  (fn [db [_ count]]
    (assoc-in db [:query-builder :query] nil)))

(reg-event
  :add-constraint
  (fn [db [_ constraint]]
    (update-in db [:query-builder :query :where]
               (fn [where]
                 (conj where constraint)))))

(reg-fx
  :run-query
  (fn [query]
    (println "running query" query)
    (go (dispatch [:handle-count (<! (search/raw-query-rows
                                       {:root "www.flymine.org/query"}
                                       query
                                       {:format "count"}))]))))

(reg-event-fx
  :qb-run-query
  (fn [{db :db}]
    (let [query-data (-> db :query-builder :query)]
      {:db        db
       :run-query (-> query-data
                      (update :select (fn [views] (map (fn [view] (clojure.string/join "." view)) views)))
                      (update :where (fn [cons]
                                       (map (fn [con]
                                              {:path (clojure.string/join "." (:path con))
                                               :op (:op con)
                                               :value (:value con)}) cons))))})))

(reg-event
  :qb-make-tree
  (fn [db]
    (let [model (-> db :assets :model)]
      (assoc-in db [:query-builder :query :from] "Gene"))))

(reg-event
  :qb-add-view
  (fn [db [_ path-vec]]
    (update-in db [:query-builder :query :select] (fn [views]
                                                    (if (some #(= % path-vec) views)
                                                      (remove #(= % path-vec) views)
                                                      (conj views path-vec))))))