(ns re-frame-boiler.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.assets :as assets]
            [re-frame-boiler.sections.objects.handlers]))

(reg-event
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event
  :set-active-panel
  (fn [db [_ active-panel panel-params]]
    (assoc db :active-panel active-panel
              :panel-params panel-params)))

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
  :async-assoc
  (fn [db [_ location-vec val]]
    (assoc-in db location-vec val)))

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

(reg-fx
  :fetch-assets
  (fn [connection]
    (go (dispatch [:async-assoc [:assets :templates] (<! (assets/templates connection))]))
    ;(go (dispatch [:async-assoc [:assets :lists] (<! (assets/lists connection))]))
    ;(go (dispatch [:async-assoc [:assets :model] (<! (assets/model connection))]))
    ))

(reg-event-fx
  :fetch-all-assets
  (fn [{db :db}]
    {:db           (assoc db :fetching-assets? true)
     :fetch-assets {:root "www.flymine.org/query"}}))

(reg-event
  :select-template
  (fn [db [_ id]]
    (assoc db :selected-template id)))

(reg-event
  :test-progress-bar
  (fn [db [_ percent]]
    (assoc db :progress-bar-percent percent)))

