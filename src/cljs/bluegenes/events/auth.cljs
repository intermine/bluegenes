(ns bluegenes.events.auth
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [bluegenes.route :as route]))

(def error-messages {401 "Invalid username or password"
                     404 "Remote server not found"})

(reg-event-fx
 ::login
 ;; Fire events to log in a user
 (fn [{db :db} [_ credentials]]
   {:db (update-in db [:mines (:current-mine db) :auth] assoc
                   :thinking? true
                   :error? nil)
    ::fx/http {:uri "/api/auth/login"
               :method :post
               :on-success [::login-success]
               :on-failure [::login-failure]
               :on-unauthorised [::login-failure]
               :transit-params credentials}}))

(reg-event-fx
 ::login-success
 ;; Store a user's identity and assoc their token to the service of the current mine,
 ;; then (re)fetch the user's lists and their MyMine labels
 (fn [{db :db} [_ {:keys [token] :as identity}]]
   (let [current-mine (:current-mine db)]
     {:db (-> db
              (update-in [:mines current-mine :auth] assoc
                         :thinking? false
                         :identity identity
                         :message nil
                         :error? false)
              (assoc-in [:mines current-mine :service :token] token))
      :dispatch-n [[:save-login current-mine identity]
                   [:assets/fetch-lists]
                   [::route/navigate ::route/home {:mine current-mine}]]})))

(reg-event-db
 ::login-failure
 ;; Clear a user's identity and store an error message
 (fn [db [_ {:keys [statusCode] :as _r}]]
   (let [msg (get error-messages statusCode "Error")]
     (update-in db [:mines (:current-mine db) :auth] assoc
                :thinking? false
                :identity nil
                :error? true
                :message msg))))

(reg-event-fx
 ::logout
 ;; Fire events to log out a user. This clears the Session on the server
 (fn [{db :db} [_]]
   {:db (update-in db [:mines (:current-mine db) :auth] assoc
                   :thinking? true)
    ::fx/http {:uri "/api/auth/logout"
               :method :get
               :on-success [::logout-success]}}))

(reg-event-fx
 ::logout-success
 ;; Clear the user's identity and reboot the application
 (fn [{db :db} [_ _response]]
   (let [current-mine (:current-mine db)]
     {:db (-> db
              (update-in [:mines current-mine :auth] assoc
                         :thinking? false
                         :identity nil
                         :error? false
                         :message nil)
              (assoc-in [:mines current-mine :service :token] nil))
      :dispatch-n [[:remove-login current-mine]
                   [:reboot]]})))
