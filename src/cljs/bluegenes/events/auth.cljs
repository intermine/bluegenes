(ns bluegenes.events.auth
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [bluegenes.route :as route]
            [imcljs.auth :as im-auth]
            [bluegenes.interceptors :refer [origin]]
            [bluegenes.config :refer [server-vars]]))

(defn slim-service
  "Constrains a service map to only the keys needed by the backend API."
  [service]
  (select-keys service [:root :token]))

(defn renamedLists->message [renamedLists]
  [:messages/add
   {:markup [:div
             [:p "The following lists have been renamed due to their name conflicting with an existing list."]
             (into [:ul]
                   (for [[old-kw new-name] renamedLists]
                     [:li (name old-kw) " → " new-name]))]
    :timeout 15000
    :style "info"}])

(reg-event-fx
 ::login
 ;; Fire events to log in a user
 (fn [{db :db} [_ username password]]
   (let [current-mine (:current-mine db)
         service (get-in db [:mines current-mine :service])]
     {:db (update-in db [:mines current-mine :auth] assoc
                     :thinking? true
                     :error? false)
      ::fx/http {:uri "/api/auth/login"
                 :method :post
                 :on-success [::login-success]
                 :on-failure [::login-failure]
                 :on-unauthorised [::login-failure]
                 :transit-params {:username username
                                  :password password
                                  :service (slim-service service)}}})))

(reg-event-fx
 ::login-success
 ;; Store a user's identity and assoc their token to the service of the current mine,
 ;; then (re)fetch the user's lists.
 (fn [{db :db} [_ {{:keys [token] :as identity} :identity ?renamedLists :renamedLists}]]
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
              (update-in [:lists :by-id] empty)
              ;; Since both :assets/fetch-lists and start-router is run below,
              ;; this causes lists to be denormalized based on previous list
              ;; data when router starts, and for it to get updated when lists
              ;; are fetched, causing user lists to be marked as new.
              (update-in [:assets :lists current-mine] empty)
              (route/force-controllers-rerun))
      :dispatch-n [[:save-login current-mine identity]
                   [:assets/fetch-lists]
                   [:assets/fetch-templates]
                   (when (seq ?renamedLists)
                     (renamedLists->message ?renamedLists))
                   ;; Restart router to rerun controllers.
                   [:bluegenes.events.boot/start-router]]
      ;; This also tracks sign-ups, which immediately follows with login-success.
      :track-event ["login"]})))

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
                 :transit-params {:service (slim-service (get-in db [:mines current-mine :service]))}}})))

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
              (assoc-in [:mines current-mine :service :token] nil)
              (route/force-controllers-rerun))
      :dispatch-n [[:remove-login current-mine]
                   [:reboot]]})))

(reg-event-fx
 ::register
 (fn [{db :db} [_ username password]]
   (let [current-mine (:current-mine db)
         service (get-in db [:mines current-mine :service])]
     {:db (update-in db [:mines current-mine :auth] assoc
                     :thinking? true
                     :error? false)
      ::fx/http {:uri "/api/auth/register"
                 :method :post
                 :on-success [::login-success]
                 :on-failure [::login-failure]
                 :on-unauthorised [::login-failure]
                 :transit-params {:username username
                                  :password password
                                  :service (slim-service service)}}})))

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
     {:db (assoc-in db [:mines (:current-mine db) :auth :reset-password-in-progress?] true)
      :im-chan {:chan (im-auth/password-reset service new-password token)
                :on-success [::reset-password-success]
                :on-failure [::reset-password-failure]
                :on-unauthorised [::reset-password-failure]}})))

(reg-event-db
 ::reset-password-success
 (fn [db [_]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :reset-password-in-progress? false
              :reset-password-success? true
              :reset-password-error nil)))

(reg-event-db
 ::reset-password-failure
 (fn [db [_ {:keys [status] :as res}]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :reset-password-in-progress? false
              :reset-password-success? false
              :reset-password-error (if (= status 405)
                                      "This feature is not supported in this version of Intermine"
                                      (or (get-in res [:body :error])
                                          "Failed to reset password")))))

(reg-event-db
 ::clear-reset-password-page
 (fn [db]
   (update-in db [:mines (:current-mine db) :auth] dissoc
              :reset-password-in-progress?
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
 [(origin)]
 (fn [{db :db origin :origin} [_ provider]]
   (let [current-mine (:current-mine db)
         service (get-in db [:mines current-mine :service])
         redirect_uri (str origin
                           (str (:bluegenes-deploy-path @server-vars)
                                "/api/auth/oauth2callback?provider=")
                           provider)]
     {:db (update-in db [:mines current-mine :auth] assoc
                     :error? false)
      ::fx/http {:uri "/api/auth/oauth2authenticator"
                 :method :post
                 :on-success [::oauth2-success redirect_uri]
                 :on-failure [::oauth2-failure]
                 :on-unauthorised [::oauth2-failure]
                 :transit-params {:service (slim-service service)
                                  :mine-id (name current-mine)
                                  :provider provider
                                  :redirect_uri redirect_uri}}})))

(reg-event-fx
 ::oauth2-success
 (fn [{db :db} [_ redirect_uri link]]
   {:external-redirect (str link "&redirect_uri=" (js/encodeURIComponent redirect_uri))}))

(reg-event-db
 ::oauth2-failure
 (fn [db [_ res]]
   (update-in db [:mines (:current-mine db) :auth] assoc
              :error? true
              :message (get-in res [:body :error]))))
