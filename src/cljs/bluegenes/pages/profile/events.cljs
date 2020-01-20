(ns bluegenes.pages.profile.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.auth :as auth]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]))

(reg-event-fx
 ::load-profile
 (fn [_]
   {:dispatch-n [[::clear-responses]
                 [::reset-preferences]
                 [::get-preferences]]}))

(reg-event-db
 ::clear-responses
 (fn [db]
   (update db :profile dissoc :responses)))

(reg-event-fx
 ::change-password
 (fn [{db :db} [_ new-password]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (update-in db [:profile :responses] dissoc :change-password)
      :im-chan {:chan (auth/change-password service new-password)
                :on-success [::change-password-success]
                :on-failure [::change-password-failure]}})))

(reg-event-db
 ::change-password-success
 (fn [db [_ _res]]
   (assoc-in db [:profile :responses :change-password]
             {:type :success
              :message "Password changed successfully."})))

(reg-event-db
 ::change-password-failure
 (fn [db [_ res]]
   (assoc-in db [:profile :responses :change-password]
             {:type :failure
              :message (or (get-in res [:body :error])
                           "Failed to change password. Please check your connection and try again.")})))

(reg-event-fx
 ::get-preferences
 (fn [{db :db}]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (assoc-in db [:profile :requests :get-preferences] true)
      :im-chan {:chan (fetch/preferences service)
                :on-success [::get-preferences-success]
                :on-failure [::get-preferences-failure]}})))

(def default-preferences
  {;; Do not be informed by email of newly shared lists.
   :do_not_spam false
   ;; You won't be visible when users attempt to share a list.
   :hidden false
   ;; Public name users can use to share lists with you if not `hidden`.
   :alias ""
   ;; Email address which receives updates on shared lists.
   :email ""
   ;; The URL of the Galaxy instance to be used when sharing data to Galaxy.
   :galaxy-url ""})

;; Reset user preference inputs back to the last fetched values.
(reg-event-db
 ::reset-preferences
 (fn [db]
   (assoc-in db [:profile :inputs :user-preferences]
             (get-in db [:profile :preferences]))))

;; If we later need to add support for reading Clojure data structures, we can
;; use `cljs.reader/read-string`. It is safe since it's only an EDN reader in
;; ClojureScript, meaning it won't evaluate code.
(defn read-number-boolean-string
  "Parse a value, returning one of the following if value satisfies the clause:
  - value: Value is not a string.
  - number: String entirely composed of numbers with an optional decimal.
  - boolean: String exactly matches either 'true' or 'false'.
  - string: String doesn't satisfy any of the above clauses."
  [s]
  (if (not (string? s))
    s
    (let [number (js/parseFloat s 10)]
      (if (or (js/isNaN number)
              (nil? (re-matches #"\d*\.?\d*" s)))
        (case s
          "true" true
          "false" false
          s)
        number))))

(defn parse-preferences
  "Preferences are saved as string values on the server, so we need to parse
  them as actual data when receiving them."
  [prefs]
  (reduce (fn [m k]
            (update m k read-number-boolean-string))
          prefs
          (keys prefs)))

(reg-event-db
 ::get-preferences-success
 (fn [db [_ prefs]]
   (let [prefs' (merge default-preferences (parse-preferences prefs))]
     (-> db
         (update-in [:profile :requests] dissoc :get-preferences)
         (assoc-in [:profile :preferences] prefs')
         (assoc-in [:profile :inputs :user-preferences] prefs')))))

(reg-event-db
 ::get-preferences-failure
 (fn [db [_ res]]
   (-> db
       (update-in [:profile :requests] dissoc :get-preferences)
       (assoc-in [:profile :responses :user-preferences]
                 {:type :failure
                  :message (or (get-in res [:body :error])
                               "Error occured when acquiring preferences.")}))))

(reg-event-db
 ::set-input
 (fn [db [_ path value]]
   (assoc-in db (into [:profile :inputs] path) value)))

(reg-event-db
 ::update-input
 (fn [db [_ path f]]
   (update-in db (into [:profile :inputs] path) f)))

(reg-event-fx
 ::save-preferences
 (fn [{db :db} [_ new-prefs]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (update-in db [:profile :responses] dissoc :user-preferences)
      :im-chan {:chan (save/preferences service new-prefs)
                :on-success [::save-preferences-success]
                :on-failure [::save-preferences-failure]}})))

(reg-event-db
 ::save-preferences-success
 (fn [db [_ prefs]]
   (let [prefs' (merge default-preferences (parse-preferences prefs))]
     (-> db
         (assoc-in [:profile :preferences] prefs')
         (assoc-in [:profile :inputs :user-preferences] prefs')
         (assoc-in [:profile :responses :user-preferences]
                   {:type :success
                    :message "Updated preferences have been saved."})))))

(reg-event-db
 ::save-preferences-failure
 (fn [db [_ res]]
   (assoc-in db [:profile :responses :user-preferences]
             {:type :failure
              :message (or (get-in res [:body :error])
                           "Error occured when saving preferences.")})))

(reg-event-fx
 ::start-deregistration
 (fn [{db :db}]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (assoc-in db [:profile :requests :start-deregistration] true)
      :im-chan {:chan (auth/deregistration service)
                :on-success [::start-deregistration-success]
                :on-failure [::start-deregistration-failure]}})))

(reg-event-db
 ::start-deregistration-success
 (fn [db [_ token]]
   (-> db
       (update-in [:profile :requests] dissoc :start-deregistration)
       (assoc-in [:profile :responses :deregistration-token] (:uuid token)))))

(reg-event-db
 ::start-deregistration-failure
 (fn [db [_ res]]
   (-> db
       (update-in [:profile :requests] dissoc :start-deregistration)
       (assoc-in [:profile :responses :deregistration]
                 {:type :failure
                  :message (or (get-in res [:body :error])
                               "Error occured when acquiring deregistration token.")}))))

(reg-event-fx
 ::delete-account
 (fn [{db :db} [_ deregistration-token]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (assoc-in db [:profile :requests :delete-account] true)
      :im-chan {:chan (auth/delete-account service deregistration-token)
                :on-success [::delete-account-success]
                :on-failure [::delete-account-failure]}})))

(reg-event-fx
 ::delete-account-success
 (fn [{db :db} [_ _res]]
   {:db (update-in db [:profile :requests] dissoc :delete-account)
    :dispatch [:bluegenes.events.auth/logout]}))

(reg-event-db
 ::delete-account-failure
 (fn [db [_ res]]
   (-> db
       (update-in [:profile :requests] dissoc :delete-account)
       (assoc-in [:profile :responses :delete-account]
                 {:type :failure
                  :message (or (get-in res [:body :error])
                               (when (= 400 (:status res))
                                 "The code is either invalid or has expired.")
                               "Error occured when deleting account.")}))))
