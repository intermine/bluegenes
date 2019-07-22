(ns bluegenes.components.tools.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.effects :as fx]))

(reg-event-fx
 ::fetch-tools
 (fn [{db :db} [_]]
   {:db db
    ::fx/http {:method :get
               :on-success [::store-tools]
               :uri (str "/api/tools/all")}}))

(reg-event-db
 ::store-tools
 (fn [db [_ tools]]
   (assoc-in db [:tools :all] (:tools tools))))
