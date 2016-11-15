(ns redgenes.components.idresolver.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.idresolver :as idresolver]
            [imcljsold.filters :as filters]
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
  (fn [[id service extra]]
    (let [job (idresolver/resolve
                service
                {:identifiers (if (seq? id) id [id])
                 :type        "Gene"
                 :extra       extra})]
      (go (dispatch [:handle-id (<! job)])))))

(reg-event-fx
  :idresolver/resolve
  (fn [{db :db} [_ id]]
    (let [service  (get-in db [:mines (get db :current-mine) :service])
          organism (get-in db [:mines (get db :current-mine) :abbrev])]
      {:db
       (-> db
           (assoc-in [:idresolver :resolving?] true)
           (update-in [:idresolver :bank]
                      (fn [bank]
                        (distinct (reduce (fn [total next]
                                            (conj total {:input  next
                                                         :status :inactive})) bank id)))))
       :idresolver/resolve-id
       [id service organism]})))

(defn toggle-into-collection [coll val]
  (if-let [found (some #{val} coll)]
    (remove #(= % found) coll)
    (conj coll val)))

(reg-event-db
  :idresolver/toggle-selected
  (fn [db [_ id]]
    (let [multi-select? (get-in db [:idresolver :select-multi])]
      (if multi-select?
        (update-in db [:idresolver :selected] toggle-into-collection id)
        (if (= id (first (get-in db [:idresolver :selected])))
          (assoc-in db [:idresolver :selected] [])
          (assoc-in db [:idresolver :selected] [id]))))))

(reg-event-db
  :idresolver/remove-from-bank
  (fn [db [_ selected]]
    (assoc-in db [:idresolver :bank]
              (reduce (fn [total next]
                        (if-not (some? (some #{(:input next)} selected))
                          (conj total next)
                          total)) [] (get-in db [:idresolver :bank])))))

(reg-event-db
  :idresolver/remove-from-results
  (fn [db [_ selected]]
    (update-in db [:idresolver :results] (partial apply dissoc) selected)))

(reg-event-fx
  :idresolver/delete-selected
  (fn [{db :db}]
    (let [selected (get-in db [:idresolver :selected])]
      {:db         (assoc-in db [:idresolver :selected] '())
       :dispatch-n [[:idresolver/remove-from-bank selected]
                    [:idresolver/remove-from-results selected]]})))

(reg-event-db
  :idresolver/clear-selected
  (fn [db]
    (assoc-in db [:idresolver :selected] '())))

(reg-event-db
  :idresolver/clear
  (fn [db]
    (update-in db [:idresolver] assoc
               :bank nil
               :results nil
               :resolving? false
               :selected '())))

(reg-event-db
  :idresolver/toggle-select-multi
  (fn [db [_ tf]]
    (assoc-in db [:idresolver :select-multi] tf)))

(reg-event-db
  :idresolver/toggle-select-range
  (fn [db [_ tf]]
    (assoc-in db [:idresolver :select-range] tf)))

(reg-event-fx
  :idresolver/save-results
  (fn [{db :db}]
    (let [ids     (remove nil? (map (fn [[_ {id :id}]] id) (-> db :idresolver :results)))
          results {:sd/type    :query
                   :sd/service :flymine
                   :sd/label   (str "Uploaded " (count ids) " Genes")
                   :sd/value   {:from   "Gene"
                                :title  (str "Uploaded " (count ids) " Genes")
                                :select (get-in db [:assets :summary-fields :Gene])
                                :where  [{:path   "Gene.id"
                                          :op     "ONE OF"
                                          :values ids}]}}]
      {:db       db
       :dispatch [:save-data results]
       ;:navigate "saved-data"
       })))


(reg-event-fx
  :idresolver/analyse
  (fn [{db :db}]
    (let [uid            (str (gensym))
          ids            (remove nil? (map (fn [[_ {id :id}]] id) (-> db :idresolver :results)))
          summary-fields (get-in db [:assets :summary-fields :Gene])
          results        {:type  :query
                          :label (str "Uploaded " (count ids) " Genes")
                          :value {:title  (str "Uploaded " (count ids) " Genes")
                                  :from   "Gene"
                                  :select summary-fields
                                  :where  [{:path   "Gene.id"
                                            :op     "ONE OF"
                                            :values ids}]}}]
      {:dispatch [:results/set-query {:source (get db :current-mine)
                                      :type   :query
                                      :value  (:value results)}]
       :navigate (str "results")})))



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
