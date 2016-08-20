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
    (println "handling id" id)
    db))

(reg-fx
  :resolve-id
  (fn [id]
    (let [job (idresolver/resolve
                {:root "www.flymine.org/query"}
                {:identifiers [id]
                 :type        "Gene"})]
      (go (dispatch [:handle-id (<! job)])))))

(reg-event-fx
  :idresolver/resolve
  (fn [{db :db} [_ id]]
    {:db         (-> db
                     (assoc-in [:idresolver :resolving?] true)
                     (update-in [:idresolver :bank] conj id))
     :resolve-id id}))