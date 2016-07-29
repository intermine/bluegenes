(ns re-frame-boiler.handlers
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx]]
            [re-frame-boiler.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(reg-event
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

(reg-event
  :good-who-am-i
  (fn [db [_ result]]
    (assoc db :who-am-i (:user result))))

(reg-event-fx
  :log-in
  (fn [{:keys [db]} _]
    {:db         (assoc db :show-twirly true)
     :http-xhrio {:method          :get
                  :uri             "http://www.flymine.org/query/service/user/whoami"
                  :params          {:token "TokenHere"}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-who-am-i]
                  :on-failure      [:bad-http-result]}}))

(reg-event
  :log-out
  (fn [db]
    (dissoc db :who-am-i)))
