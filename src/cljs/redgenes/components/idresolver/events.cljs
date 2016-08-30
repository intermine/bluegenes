(ns redgenes.components.idresolver.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.idresolver :as idresolver]
            [imcljs.filters :as filters]
            [com.rpl.specter :as s]
            [accountant.core :refer [navigate!]]
            [clojure.zip :as zip]))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(reg-event-db
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
  :idresolver/resolve-id
  (fn [id]
    (let [job (idresolver/resolve
                {:root @(subscribe [:mine-url])}
                {:identifiers (if (seq? id) id [id])
                 :type        "Gene"
                 :extra       "D. melanogaster"})]
      (go (dispatch [:handle-id (<! job)])))))

(reg-event-fx
  :idresolver/resolve
  (fn [{db :db} [_ id]]
    {:db                    (-> db
                                (assoc-in [:idresolver :resolving?] true)
                                (update-in [:idresolver :bank]
                                           (fn [bank]
                                             (reduce (fn [total next]
                                                       (conj total {:input  next
                                                                    :status :inactive})) bank id))))
     :idresolver/resolve-id id}))

(reg-event-db
  :idresolver/clear
  (fn [db]
    (update-in db [:idresolver] assoc
               :bank nil
               :results nil
               :resolving? false)))

(reg-event-fx
  :idresolver/save-results
  (fn [{db :db}]
    (let [ids     (remove nil? (map (fn [[_ {id :id}]] id) (-> db :idresolver :results)))
          results {:type  :query
                   :label (str "Uploaded " (count ids) " Genes")
                   :value {:from   "Gene"
                           :select "*"
                           :where  [{:path   "id"
                                     :op     "ONE OF"
                                     :values ids}]}}]
      {:db       db
       :dispatch [:save-data results]
       :navigate "saved-data"})))

(reg-fx
  :navigate
  (fn [url]
    (navigate! (str "#/" url))))

(reg-event-fx
  :idresolver/analyse
  (fn [{db :db}]
    (let [uid (str (gensym))]
      {:db       (update-in db [:idresolver :saved]
                            (fn [saved]
                              (assoc saved uid (remove nil? (map (fn [[input {id :id}]] id) (-> db :idresolver :results))))))
       :navigate (str "listanalysis/temp/" uid)})))

(reg-event-db
  :idresolver/resolve-duplicate
  (fn [db [_ input result]]
    (let [symbol (:symbol (:summary result))]
      (-> db
          ; Associate the chosen result to the results map using its symbol
          (assoc-in [:idresolver :results symbol]
                    (assoc result
                      :input [symbol]
                      :status :MATCH))
          ; Dissociate the old {:input {...}} result
          (dissoc-in [:idresolver :results input])
          ; Replace the old input with the new input
          (update-in [:idresolver :bank]
                     (fn [bank]
                       (map (fn [next]
                              (if (= input (:input next))
                                {:input  symbol
                                 :status :inactive}
                                next)) bank)))))))
