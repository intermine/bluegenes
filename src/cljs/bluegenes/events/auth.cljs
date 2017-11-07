(ns bluegenes.events.auth
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [subscribe reg-event-db reg-event-db reg-event-fx]]
            [cljs.core.async :refer [<!]]
            [imcljs.auth :as im-auth]))

(def error-messages {401 "Invalid username or password"
                     404 "Remote server not found"})

(reg-event-fx
  ::login
  (fn [{db :db} [_ {:keys [username password] :as credentials}]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/login"
            :method :post
            :on-success [::login-success]
            :on-failure [::login-failure]
            :params credentials}}))

(reg-event-fx
  ::logout
  (fn [{db :db} [_]]
    {:db (assoc-in db [:auth :thinking?] true)
     :http {:uri "/api/auth/logout"
            :method :get
            :on-success [::logout-success]
            :on-denied [::logout-fail]}}))


; TODO
; @(subscribe) is too stateful. Revisit.
(reg-event-fx
  ::login-success
  (fn [{db :db} [_ {token :token :as identity}]]
    {:db (-> db
             (update :auth assoc
                     :thinking? false
                     :identity identity
                     :message nil
                     :error? false)
             (assoc-in [:mines (:id @(subscribe [:current-mine])) :service :token] token))
     :dispatch-n [[:assets/fetch-lists]
                  [:bluegenes.events.mymine/fetch-tree]]}))

(reg-event-db
  ::login-failure
  (fn [db [_ {:keys [statusCode]}]]
    (let [msg (get error-messages statusCode "Error")]
      (update db :auth assoc
              :thinking? false
              :identity nil
              :error? true
              :message msg))))

(reg-event-fx
  ::logout-success
  (fn [{db :db} [_ response]]
    {:db (update db :auth assoc
                 :thinking? false
                 :identity nil
                 :error? false
                 :message nil)
     :dispatch [:boot]}))




