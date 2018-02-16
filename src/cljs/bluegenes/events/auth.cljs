(ns bluegenes.events.auth
  (:require [re-frame.core :refer [subscribe reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]))

(def error-messages {401 "Invalid username or password"
                     404 "Remote server not found"})

(defn login-fn
  "Fire events to log in a user"
  [{db :db} [_ {:keys [username password] :as credentials}]]
  {:db (assoc-in db [:auth :thinking?] true)
   ::fx/http {:uri "/api/auth/login"
              :method :post
              :on-success [::login-success]
              :on-failure [::login-failure]
              :transit-params credentials}})

(defn logout-fn
  "Fire events to log out a user. This clears the Session on the server"
  [{db :db} [_]]
  {:db (assoc-in db [:auth :thinking?] true)
   ::fx/http {:uri "/api/auth/logout"
              :method :get
              :on-success [::logout-success]}})

(defn login-success-fn
  "Store a user's identity and assoc their token to the service of the current mine,
  then (re)fetch the user's lists and their MyMine labels
  ; TODO - factor out the subscribe"
  [{db :db} [_ {token :token :as identity}]]
  {:db (-> db
           (update :auth assoc
                   :thinking? false
                   :identity identity
                   :message nil
                   :error? false)
           (assoc-in [:mines (:id @(subscribe [:current-mine])) :service :token] token))
   :dispatch-n [[:assets/fetch-lists]
                ; TODO - remove tags
                #_[:bluegenes.sections.mymine.events/fetch-tree]
                ]})

(defn login-failure-fn
  "Clear a user's identity and store an error message"
  [db [_ {:keys [statusCode]}]]
  (let [msg (get error-messages statusCode "Error")]
    (update db :auth assoc
            :thinking? false
            :identity nil
            :error? true
            :message msg)))

(defn logout-success-fn
  "Clear the user's identity and reboot the application"
  [{db :db} [_ response]]
  {:db (update db :auth assoc
               :thinking? false
               :identity nil
               :error? false
               :message nil)
   :dispatch [:boot]})

(reg-event-fx ::login login-fn)
(reg-event-fx ::login-success login-success-fn)
(reg-event-db ::login-failure login-failure-fn)

(reg-event-fx ::logout logout-fn)
(reg-event-fx ::logout-success logout-success-fn)