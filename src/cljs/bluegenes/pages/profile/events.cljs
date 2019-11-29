(ns bluegenes.pages.profile.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.auth :as auth]))

(reg-event-db
 ::clear-responses
 (fn [db]
   (update db :profile dissoc :responses)))

(reg-event-fx
 ::change-password
 (fn [{db :db} [_ new-password]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (auth/change-password service new-password)
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
