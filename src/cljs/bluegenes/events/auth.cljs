(ns bluegenes.events.auth
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(reg-event-db
  ::login-success
  (fn [db [_ response]]
    (js/console.log response)
    (assoc db :who-am-i response)))

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
  ::register
  (fn [{db :db} [_ {:keys [email password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/register"
            :method :post
            :on-success [::login-success]
            :on-denied [::register-fail]
            :params credentials}}))