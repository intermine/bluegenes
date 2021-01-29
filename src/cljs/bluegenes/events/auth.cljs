(ns bluegenes.events.auth
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [bluegenes.route :as route]
            [imcljs.auth :as im-auth]
            [bluegenes.interceptors :refer [origin]]))

(reg-event-fx
 ::login
 ;; Fire events to log in a user
 (fn [{db :db} [_ credentials]]
   {:db (update-in db [:mines (:current-mine db) :auth] assoc
                   :thinking? true
                   :error? false)
    ::fx/http {:uri "/api/auth/login"
               :method :post
               :on-success [::login-success]
               :on-failure [::login-failure]
               :on-unauthorised [::login-failure]
               :transit-params credentials}}))

(reg-event-fx
 ::login-success
 ;; Store a user's identity and assoc their token to the service of the current mine,
 ;; then (re)fetch the user's lists.
 (fn [{db :db} [_ {:keys [token] :as identity}]]
   (let [current-mine (:current-mine db)]
     {:db (-> db
              (update-in [:mines current-mine :auth] assoc
                         :thinking? false
                         :identity identity
                         :message nil
                         :error? false)
              (assoc-in [:mines current-mine :service :token] token)
              ;; The old by-id map is used to detect newly added lists.
              ;; We clear it here as otherwise all the lists belonging to the
              ;; user will be annotated as new.
              (update-in [:lists :by-id] empty))
      :dispatch-n [[:save-login current-mine identity]
                   [:assets/fetch-lists]]})))

(reg-event-db
 ::login-failure
 ;; Clear a user's identity and store an error message
 (fn [db [_ res]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :thinking? false
              :identity nil
              :error? true
              :message (get-in res [:body :error]))))

(reg-event-fx
 ::logout
 ;; Fire events to log out a user. This clears the Session on the server
 (fn [{db :db} [_]]
   (let [current-mine (:current-mine db)]
     {:db (update-in db [:mines current-mine :auth] assoc
                     :thinking? true)
      ::fx/http {:uri "/api/auth/logout"
                 :method :post
                 :on-success [::logout-success]
                 ;; We don't really care if anything goes wrong.
                 :on-failure [::logout-success]
                 :on-unauthorised [::logout-success]
                 :transit-params {:service (select-keys
                                            (get-in db [:mines current-mine :service])
                                            [:root :token])}}})))

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
                   [::route/navigate ::route/home]
                   [:reboot]]})))

(reg-event-fx
 ::register
 (fn [{db :db} [_ credentials]]
   {:db (update-in db [:mines (:current-mine db) :auth] assoc
                   :thinking? true
                   :error? false)
    ::fx/http {:uri "/api/auth/register"
               :method :post
               :on-success [::login-success]
               :on-failure [::login-failure]
               :on-unauthorised [::login-failure]
               :transit-params credentials}}))

(reg-event-fx
 ::request-reset-password
 [(origin)]
 (fn [{db :db origin :origin} [_ email]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         redirectUrl (str origin (route/href ::route/resetpassword))]
     {:db (update-in db [:mines (:current-mine db) :auth] assoc
                     :thinking? true)
      :im-chan {:chan (im-auth/request-password-reset service email redirectUrl)
                :on-success [::request-reset-password-success]
                :on-failure [::request-reset-password-failure]}})))

(reg-event-db
 ::request-reset-password-success
 (fn [db [_]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :thinking? false
              :error? false
              :message nil
              :request-reset-success? true)))

(reg-event-db
 ::request-reset-password-failure
 (fn [db [_ res]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :thinking? false
              :error? true
              :message (if (empty? res)
                         "This feature is not supported in this version of Intermine"
                         (or (get-in res [:body :error])
                             "Failed to send recovery email"))
              :request-reset-success? false)))

(reg-event-fx
 ::reset-password
 (fn [{db :db} [_ new-password token]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (im-auth/password-reset service new-password token)
                :on-success [::reset-password-success]
                :on-failure [::reset-password-failure]
                :on-unauthorised [::reset-password-failure]}})))

(reg-event-db
 ::reset-password-success
 (fn [db [_]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :reset-password-success? true
              :reset-password-error nil)))

(reg-event-db
 ::reset-password-failure
 (fn [db [_ {:keys [status] :as res}]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :reset-password-success? false
              :reset-password-error (if (= status 405)
                                      "This feature is not supported in this version of Intermine"
                                      (or (get-in res [:body :error])
                                          "Failed to reset password")))))

(reg-event-db
 ::clear-reset-password-page
 (fn [db]
   (update-in db [:mines (:current-mine db) :auth] dissoc
              :reset-password-success?
              :reset-password-error)))

(reg-event-db
 ::clear-error
 (fn [db]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :error? false
              :message nil
              :request-reset-success? false)))

(reg-event-fx
 ::oauth2
 (fn [{db :db} [_ provider]]
   (let [current-mine (:current-mine db)
         service (get-in db [:mines current-mine :service])]
     {:db (update-in db [:mines current-mine :auth] assoc
                     :error? false)
      ::fx/http {:uri "/api/auth/oauth2authenticator"
                 :method :post
                 :on-success [::oauth2-success provider]
                 :on-failure [::oauth2-failure]
                 :on-unauthorised [::oauth2-failure]
                 :transit-params {:service (select-keys service [:root :token])
                                  :mine-id (name current-mine)
                                  :provider provider}}})))

(reg-event-fx
 ::oauth2-success
 [(origin)]
 (fn [{db :db origin :origin} [_ provider link]]
   {:external-redirect (str link "&redirect_uri="
                            (js/encodeURIComponent (str origin "/api/auth/oauth2callback"
                                                        "?provider=" provider)))}))

(reg-event-db
 ::oauth2-failure
 (fn [db [_ res]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :error? true
              :message (get-in res [:body :error]))))
