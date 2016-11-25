(ns redgenes.sections.lists.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [day8.re-frame.http-fx]
            [accountant.core :refer [navigate!]]
            [redgenes.interceptors :refer [clear-tooltips]]
            [redgenes.effects]
            [dommy.core :refer-macros [sel sel1]]
            [redgenes.sections.saveddata.events]
            [imcljs.save :as save]
            [redgenes.specs :as specs]))



(reg-event-fx
  :lists/success-union
  (fn [{db :db} [_ m]]
    {:db       (assoc-in db [:lists :selected] #{})
     :dispatch [:assets/fetch-lists]}))

(reg-event-db
  :lists/print
  (fn [db [_ r]]
    (.log js/console (get-in db [:lists :selected]))
    db))



(reg-event-db
  :lists/select
  (fn [db [_ name add?]]
    (if add?
      (update-in db [:lists :selected] (comp set conj) name)
      (update-in db [:lists :selected] (partial remove #{name})))))

(reg-event-fx
  :im-operation-handler
  (fn [{db :db} [_ op-map]]
    {:db           db
     :im-operation op-map}))

(reg-event-fx
  :lists/delete
  (fn [{db :db}]
    {:db db
     :dispatch-n
         (into []
               (map (fn [list-name]
                      [:im-operation-handler
                       {:op
                        (partial save/im-list-delete (get-in db [:mines (:current-mine db) :service]) list-name)
                        :on-success
                        [:lists/success-union]}])
                    (get-in db [:lists :selected])))}))


(reg-event-fx
  :lists/union
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial save/im-list-union
                                         (get-in db [:mines (:current-mine db) :service])
                                         (str "New List (" (.toString (js/Date.)) ")")
                                         (get-in db [:lists :selected]))
                    :on-success [:lists/success-union]}}))

(reg-event-db
  :lists/set-text-filter
  (fn [db [_ value]]
    (let [adjusted-value (if (= value "") nil value)]
      (assoc-in db [:lists :controls :filters :text-filter] adjusted-value))))

(reg-event-db
  :lists/toggle-sort
  (fn [db [_ column-kw]]
    (update-in db [:lists :controls :sort column-kw]
               (fn [v]
                 (case v
                   :asc :desc
                   :desc nil
                   nil :asc)))))

(defn build-list-query [type summary-fields name title]
  {:title  title
   :from   type
   :select summary-fields
   :where  [{:path  type
             :op    "IN"
             :value name}]})

(reg-event-fx
  :lists/view-results
  (fn [{db :db} [_ {:keys [type name title source]}]]
    (let [summary-fields (get-in db [:assets :summary-fields source (keyword type)])]
      {:db       db
       :dispatch [:results/set-query
                  {:source source
                   :type   :query
                   :value  (build-list-query type summary-fields name title)}]
       :navigate (str "results")})))


(reg-event-db
  :lists/clear-filters
  (fn [db]
    (-> db
        (assoc-in [:lists :controls :filters :flags] {})
        (assoc-in [:lists :controls :filters :text-filter] nil))))

(reg-event-db
  :lists/toggle-flag-filter
  (fn [db [_ column-kw]]
    (update-in db [:lists :controls :filters :flags column-kw]
               (fn [v]
                 ; Tri-state toggle
                 ;(case v
                 ;  nil true
                 ;  true false
                 ;  false nil)
                 ; Bi-state toggle
                 (case v
                   nil true
                   true nil)))))