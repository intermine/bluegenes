(ns redgenes.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [redgenes.db :as db]
            [day8.re-frame.http-fx]
            [day8.re-frame.forward-events-fx]
            [ajax.core :as ajax]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.assets :as assets]
            [redgenes.sections.objects.handlers]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as t]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-fx
  :unqueue
  (fn [{db :db}]
    (merge {:db (-> db
                    (assoc :active-panel (:active-panel (:queued db)))
                    (assoc :panel-params (:panel-params (:queued db)))
                    (dissoc db :queued))}
           (if (:and-then (:queued db))
             {:dispatch (:and-then (:queued db))}))))

(reg-event-fx
  :set-active-panel
  (fn [{db :db} [_ active-panel panel-params evt]]
    (if (:fetching-assets? db)
      ; Queue our route until the assets have been fetched
      {:db             (assoc db :queued {:active-panel active-panel
                                          :panel-params panel-params
                                          :and-then     evt})
       :forward-events {:register    :route-forwarder
                        :events      #{:finished-loading-assets}
                        :dispatch-to [:unqueue]}}
      ; Otherwise route immediately, and fire an optional post-route event
      (merge {:db (assoc db :active-panel active-panel
                            :panel-params panel-params)}
             (if evt {:dispatch evt})))))

(reg-event-db
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

(reg-event-db
  :async-assoc
  (fn [db [_ location-vec val]]
    (assoc-in db location-vec val)))

(reg-event-db
  :log-out
  (fn [db]
    (dissoc db :who-am-i)))

(reg-event-db
  :handle-suggestions
  (fn [db [_ results]]
    (assoc db :suggestion-results results)))

(reg-fx
  :suggest
  (fn [{:keys [c search-term]}]
    (if (= "" search-term)
      (dispatch [:handle-suggestions nil])
      (go (dispatch [:handle-suggestions (<! c)])))))

(reg-event-fx
  :bounce-search
  (fn [{db :db} [_ term]]
    (let [connection   {:root "www.flymine.org/query"}
          suggest-chan (search/quicksearch connection term)]
      (if-let [c (:search-term-channel db)] (close! c))
      {:db      (-> db
                    (assoc :search-term-channel suggest-chan)
                    (assoc :search-term term))
       :suggest {:c suggest-chan :search-term term}})))

(reg-event-db
  :finished-loading-assets
  (fn [db]
    (assoc db :fetching-assets? false)))

(reg-fx
  :fetch-assets
  (fn [connection]
    (let [c1        (assets/templates connection)
          c2        (assets/lists connection)
          c3        (assets/model connection)
          c4        (assets/summary-fields connection)
          locations {c1 [:assets :templates]
                     c2 [:assets :lists]
                     c3 [:assets :model]
                     c4 [:assets :summary-fields]}]
      (go-loop [channels [c1 c2 c3 c4]]
               (let [[v p] (alts! channels)]
                 (if-not (and (nil? v) (empty? channels))
                   (let [remaining (remove #(= % p) channels)]
                     (dispatch [:test-progress-bar (* 100 (/ (- 4 (count remaining)) 4))])
                     (dispatch [:async-assoc (get locations p) v])
                     (if-not (empty? remaining)
                       (recur remaining)
                       (dispatch [:finished-loading-assets])))))))))

(reg-event-fx
  :fetch-all-assets
  (fn [{db :db}]
    {:db           (assoc db :fetching-assets? true
                             :progress-bar-percent 0)
     :fetch-assets {:root "www.flymine.org/query"}}))

(reg-event-db
  :test-progress-bar
  (fn [db [_ percent]]
    (assoc db :progress-bar-percent percent)))

(reg-event-db
  :save-data
  (fn [db [_ data]]
    (assoc-in db [:saved-data (str (uuid/make-random-uuid))]

               (merge data {:created (t/now)
                            :updated (t/now)}))))