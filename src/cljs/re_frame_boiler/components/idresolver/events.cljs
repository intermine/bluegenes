(ns re-frame-boiler.components.idresolver.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.idresolver :as idresolver]
            [imcljs.filters :as filters]
            [com.rpl.specter :as s]
            [clojure.zip :as zip]))

(reg-event
  :handle-id
  (fn [db [_ id]]
    (let [{{:keys [MATCH TYPE_CONVERTED OTHER WILDCARD DUPLICATE] :as resolved} :matches
           unresolved                                                           :unresolved} id
          tagged    (remove empty? (mapcat (fn [[k v]] (map (fn [r] (assoc r :status k)) v)) resolved))
          tagged-un (reduce (fn [total next] (assoc total next {:input [next] :status :UNRESOLVED})) {} unresolved)]

      (-> db
          (update-in [:idresolver :results]
                     (fn [results]
                       (merge tagged-un
                              (reduce (fn [total next-id]
                                        (merge total
                                               (reduce (fn [n next-input]
                                                         (assoc n next-input next-id)) {}
                                                       (if (vector? (:input next-id))
                                                         (:input next-id)
                                                         [(:input next-id)]))))
                                      results tagged))))))))

(reg-fx
  :resolve-id
  (fn [id]
    (let [job (idresolver/resolve
                {:root "www.flymine.org/query"}
                {:identifiers (if (seq? id) id [id])
                 :type        "Gene"
                 :extra       "D. melanogaster"})]
      (go (dispatch [:handle-id (<! job)])))))

(reg-event-fx
  :idresolver/resolve
  (fn [{db :db} [_ id]]
    {:db         (-> db
                     (assoc-in [:idresolver :resolving?] true)
                     (update-in [:idresolver :bank]
                                (fn [bank]
                                  (reduce (fn [total next]
                                            (conj total {:input  next
                                                         :status :inactive})) bank id))))
     :resolve-id id}))

(reg-event
  :idresolver/clear
  (fn [db]
    (update-in db [:idresolver] assoc
               :bank nil
               :results nil
               :resolving? false

               )))