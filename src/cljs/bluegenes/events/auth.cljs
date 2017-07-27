(ns bluegenes.events.auth
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(reg-event-fx
  ::login
  (fn [{db :db} [_ {:keys [email password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/login"
            :method :post
            :on-success [:bluegenes.auth/login-success]
            :on-denied [:bluegenes.auth/login-denied]
            :params credentials}}))

(reg-event-fx
  ::register
  (fn [{db :db} [_ {:keys [email password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/register"
            :method :post
            :on-success [:bluegenes.auth/register-success]
            :on-denied [:bluegenes.auth/register-denied]
            :params credentials}}))