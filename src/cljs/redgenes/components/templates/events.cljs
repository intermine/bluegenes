(ns redgenes.components.templates.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [re-frame.events]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.search :as search]))


(def ns->kw (comp keyword namespace))
(def name->kw (comp keyword name))
(def ns->vec (juxt ns->kw name->kw))

(reg-event-fx
  :template-chooser/choose-template
  (fn [{db :db} [_ id]]
    (let [query (get-in db [:assets :templates (ns->kw id) (name->kw id)])]
      {:db       (update-in db [:components :template-chooser]
                            assoc
                            :selected-template query
                            :selected-template-name id
                            :selected-template-service (get-in db [:mines (ns->kw id) :service])
                            :count nil)
       :dispatch [:template-chooser/run-count]})))

(reg-event-db
  :template-chooser/set-category-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :selected-template-category] id)))

(reg-event-db
  :template-chooser/set-text-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :text-filter] id)))


(reg-event-fx
  :templates/send-off-query
  (fn [{db :db} [_]]
    (let [summary-fields (get-in db [:assets :summary-fields (keyword type)])]
      {:db       db
       :dispatch [:results/set-query
                  {:source (ns->kw (get-in db [:components :template-chooser :selected-template-name] ))
                   :type   :query
                   :value  (get-in db [:components :template-chooser :selected-template])}]
       :navigate (str "results")})))

(reg-event-fx
  :template-chooser/replace-constraint
  (fn [{db :db} [_ index value]]
    {:db       (assoc-in db [:components :template-chooser :selected-template :where index] value)
     :dispatch [:template-chooser/run-count]
     }))

(reg-event-db
  :template-chooser/update-count
  (fn [db [_ c]]
    (update-in db [:components :template-chooser] assoc
               :count c
               :counting? false)))

(reg-fx
  :template-chooser/pipe-count
  (fn [count-chan]
    (go (dispatch [:template-chooser/update-count (<! count-chan)]))))

(reg-event-fx
  :template-chooser/run-count
  (fn [{db :db}]
    (let [query      (get-in db [:components :template-chooser :selected-template])
          template-name (get-in db [:components :template-chooser :selected-template-name])
          service (get-in db [:mines (ns->kw template-name) :service])
          count-chan (search/raw-query-rows
                       service
                       query
                       {:format "count"})
          new-db     (update-in db [:components :template-chooser] assoc
                                :count-chan count-chan
                                :counting? true)]
      {:db                          new-db
       :template-chooser/pipe-count count-chan})))
