(ns re-frame-boiler.components.templates.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]))

(reg-event-db
  :template-chooser/choose-template
  (fn [db [_ id]]
    (let [template (get-in db [:assets :templates id])]
      (assoc-in db [:components :template-chooser :selected-template] template))))

(reg-event-db
  :template-chooser/set-category-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :selected-template-category] id)))

(reg-event-db
  :template-chooser/set-text-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :text-filter] id)))

(reg-event-fx
  :template-chooser/replace-constraint
  (fn [{db :db} [_ index value]]
    {:db (assoc-in db [:components :template-chooser :selected-template :where index] value)
     :dispatch [:template-chooser/run-count]}))

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
          count-chan (search/raw-query-rows
                       {:root "www.flymine.org/query"}
                       query
                       {:format "count"})
          new-db     (update-in db [:components :template-chooser] assoc
                                :count-chan count-chan
                                :counting? true)]
      {:db                          new-db
       :template-chooser/pipe-count count-chan})))