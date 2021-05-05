(ns bluegenes.pages.results.widgets.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [clojure.string :as str]
            [clojure.set :as set]
            [imcljs.fetch :as im-fetch]))

;; Enrichment widgets are handled in their own namespace.
(def supported-widget-types #{"chart" "table"})

(defn get-widget-targets
  [widget]
  (->> widget :targets (map keyword)))

(defn widgets-to-load
  [entities widgets]
  (filter (fn [widget]
            (and (contains? supported-widget-types (:widgetType widget))
                 (some entities (get-widget-targets widget))))
          widgets))

(defn mapify-widgets
  [widgets]
  (reduce #(assoc %1 (:name %2) %2) {} widgets))

(reg-event-fx
 :widgets/load
 (fn [{db :db} [_]]
   (let [entities (get-in db [:tools :entities])
         widgets (widgets-to-load entities (get-in db [:assets :widgets (:current-mine db)]))]
     {:dispatch-n (map (fn [widget]
                         [:widgets/get-widget-data widget (:value (some entities (get-widget-targets widget)))])
                       widgets)})))

(reg-event-fx
 :widgets/get-widget-data
 (fn [{db :db} [_ widget ids]]
   (let [fetch-widget (case (:widgetType widget)
                        "chart" im-fetch/chart-widget
                        "table" im-fetch/table-widget)
         widget-name (:name widget)
         service (get-in db [:mines (:current-mine db) :service])]
     ;; TODO DEBUG
     {:im-chan {:chan (fetch-widget service "Public ABC Genes (testing)" widget-name)
                :on-success [:widgets/get-widget-data-success widget-name]
                :on-failure [:widgets/get-widget-data-failure widget-name]}})))

(reg-event-db
 :widgets/get-widget-data-success
 (fn [db [_ widget-name res]]
   (assoc-in db [:results :widget-results (keyword widget-name)] res)))

(reg-event-fx
 :widgets/get-widget-data-failure
 (fn [{db :db} [_ widget-name res]]
   {:db (assoc-in db [:results :widget-results (keyword widget-name)] false)
    :log-error [(str "Failed to get " widget-name " data") res]}))
