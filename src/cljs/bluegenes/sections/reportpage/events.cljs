(ns bluegenes.sections.reportpage.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.db :as db]
            [bluegenes.effects :as fx]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [imcljs.path :as path]))

(reg-event-db
  :handle-report-summary
  (fn [db [_ summary]]
    (-> db
        (assoc-in [:report :summary] summary)
        (assoc :fetching-report? false))))

(reg-event-fx
  :fetch-report
  (fn [{db :db} [_ mine type id]]
    (let [type-kw (keyword type)
          q       {:from type
                   :select (-> db :assets :summary-fields mine type-kw)
                   :where [{:path (str type ".id")
                            :op "="
                            :value id}]}]

      {:im-chan {:chan (fetch/rows (get-in db [:mines mine :service]) q {:format "json"})
                 :on-success [:handle-report-summary]}})))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ mine type id]]
    {:db (-> db
             (assoc :fetching-report? true)
             (dissoc :report))
     :dispatch-n [[::fetch-tools nil]
                  [:fetch-report (keyword mine) type id]]}))

(reg-event-fx
 ::fetch-tools
  (fn [{db :db} [x tool-type]]
      {:db db
       ::fx/http {:method :get
              :on-success [::store-tools]
              :uri (str "/api/tools/all" )}}))

(defn aggregate-classes [m tool]
  ;;Oh my, I had help on this one. https://stackoverflow.com/questions/48010316/clojure-clojurescript-group-by-a-map-on-multiple-values/48010630#48010630
  (->> (get-in tool [:config :classes])
       (reduce (fn [acc elem]
                 (update acc elem conj tool))
               m)))

(reg-event-db
 ::store-tools
  (fn [db [_ tools]]
      (->
        (assoc-in db [:tools :all] (:tools tools))
        (assoc-in [:tools :classes] (reduce aggregate-classes {} (:tools tools))))))
