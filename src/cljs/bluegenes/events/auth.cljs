(ns bluegenes.events.auth
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(reg-event-db
  ::login-success
  (fn [db [_ response]]
    (update db :auth assoc :thinking? false :identity response)))

(reg-event-db
  ::logout-success
  (fn [db [_ response]]
    (update db :auth assoc :thinking? false :identity {})))

(reg-event-fx
  ::register
  (fn [{db :db} [_ {:keys [email password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/register"
            :method :post
            :on-success [::login-success]
            :on-denied [::register-fail]
            :params credentials}}))

(reg-event-fx
  ::login
  (fn [{db :db} [_ {:keys [email password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/login"
            :method :post
            :on-success [::login-success]
            :on-denied [::login-fail]
            :params credentials}}))

(reg-event-fx
  ::logout
  (fn [{db :db} [_]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/logout"
            :method :get
            :on-success [::logout-success]
            :on-denied [::logout-fail]}}))

