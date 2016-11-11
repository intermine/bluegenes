(ns redgenes.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx dispatch subscribe]]
            [redgenes.events.boot]
            [day8.re-frame.http-fx]
            [day8.re-frame.forward-events-fx]
            [day8.re-frame.async-flow-fx]
            [redgenes.sections.objects.handlers]
            [redgenes.components.search.events]
            [redgenes.components.databrowser.events]
            [redgenes.components.search.events :as search-full]
            [redgenes.sections.objects.handlers]
            [imcljsold.search :as search]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]))

; Change the main panel to a new view
(reg-event-fx
  :do-active-panel
  (fn [{db :db} [_ active-panel panel-params evt]]
    (cond-> {:db (assoc db
                   :active-panel active-panel
                   :panel-params panel-params)}
            evt (assoc :dispatch evt))))

; A buffer between booting and changing the view. We only change the view
; when the assets have been loaded
(reg-event-fx
  :set-active-panel
  (fn [{db :db} [_ active-panel panel-params evt]]
    (cond-> {:db db}
            (:fetching-assets? db) ; If we're fetching assets then save the panel change for later
            (assoc :forward-events {:register    :coordinator1
                                    :events      #{:finished-loading-assets}
                                    :dispatch-to [:do-active-panel active-panel panel-params evt]})
            (not (:fetching-assets? db)) ; Otherwise dispatch it now (and the optional attached event)
            (assoc :dispatch-n
                   (cond-> [[:do-active-panel active-panel panel-params evt]]
                           evt (conj evt))))))


(reg-event-fx
  :set-active-mine
  (fn [{:keys [db]} [_ value keep-existing?]]
    {:db         (cond-> (assoc db :current-mine value)
                         (not keep-existing?) (assoc-in [:assets] {}))
     :dispatch-n (list [:reboot] [:set-active-panel :home-panel])}))

(reg-event-fx
  :new-temporary-mine
  (fn [{:keys [db]} [_ new-url]]
    (let [url
          (if (clojure.string/starts-with? new-url "http://")
            (subs new-url 7)
            new-url)]
      (.log js/console "%curl" "color:hotpink;font-weight:bold;" (clj->js url))
      {:db
                 (assoc db :temporary-mine {:temporary-mine {
                                                             ;;we can make this more dynamic when we're grown up
                                                             :id      :temporary-mine
                                                             :common  "New Organism"
                                                             :status  {:status :na}
                                                             :output? true
                                                             :abbrev  "New Organism"
                                                             :mine
                                                                      {:name    "New Organism"
                                                                       :url     url
                                                                       :service {:root url}}}}
                           :mine-name :temporary-mine)
       :dispatch [:fetch-all-assets]})))

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

(reg-event-fx
  :bounce-search
  (fn [{db :db} [_ term]]
    (let [connection   (get-in db [:mines (get db :current-mine) :service])
          suggest-chan (search/quicksearch connection term 5)]
      (if-let [c (:search-term-channel db)] (close! c))
      {:db      (-> db
                    (assoc :search-term-channel suggest-chan)
                    (assoc :search-term term))
       :suggest {:c suggest-chan :search-term term :source (get db :current-mine)}})))

(reg-event-fx
  :add-toast
  (fn [db [_ message]]
    (update-in db [:toasts] conj message)))

(reg-event-db
  :test-progress-bar
  (fn [db [_ percent]]
    (assoc db :progress-bar-percent percent)))

(reg-event-db
  :cache/store-organisms
  (fn [db [_ res]]
    (assoc-in db [:cache :organisms] (:results res))))

(reg-event-fx
  :cache/fetch-organisms
  (fn [{db :db}]
    (let [model          (get-in db [:assets :model])
          organism-query {:from   "Organism"
                          :select ["name"
                                   "taxonId"
                                   "species"
                                   "shortName"
                                   "genus"
                                   "commonName"]}]
      {:db           db
       :im-operation {:op         (partial search/raw-query-rows
                                           (get-in db [:mines (:current-mine db) :service])
                                           organism-query
                                           {:format "jsonobjects"})
                      :on-success [:cache/store-organisms]}})))

;(reg-event-fx
;  :log-in
;  (fn [{:keys [db]} _]
;    {:db         (assoc db :show-twirly true)
;     :http-xhrio {:method          :get
;                  :uri             (str @(subscribe [:mine-url]) "/service/user/whoami")
;                  :params          {:token ""}
;                  :timeout         8000
;                  :response-format (ajax/json-response-format {:keywords? true})
;                  :on-success      [:good-who-am-i]
;                  :on-failure      [:bad-http-result]}}))


;(reg-event-db
;  :good-who-am-i
;  (fn [db [_ result]]
;    (assoc db :who-am-i (:user result))))
