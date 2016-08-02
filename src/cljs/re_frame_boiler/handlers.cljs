(ns re-frame-boiler.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imjs.search :as search]))

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
                  :params          {:token ""}
                  :timeout         8000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:good-who-am-i]
                  :on-failure      [:bad-http-result]}}))

(reg-event
  :log-out
  (fn [db]
    (dissoc db :who-am-i)))

(reg-event
  :handle-suggestions
  (fn [db [_ results]]
    (assoc db :suggestion-results results)))

(reg-fx
  :suggest
  (fn [val]
    (let [connection {:root "www.flymine.org/query"}]
      (if (= "" val)
        (dispatch [:handle-suggestions nil])
        (go (dispatch [:handle-suggestions (<! (search/quicksearch connection val))]))))))

(reg-event-fx
  :bounce-search
  (fn [{db :db} [_ term]]
    {:db      (assoc db :search-term term)
     :suggest term}))
