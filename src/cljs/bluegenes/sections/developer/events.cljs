(ns bluegenes.pages.developer.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [subscribe reg-event-db reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [cljs.core.async :refer [<!]]))

(reg-event-db
 ::panel
 (fn [db [_ panel-name]]
   (assoc db :debug-panel panel-name)))

(reg-event-fx
 ::panel
 (fn [{db :db} [_ panel-name]]
    ;; load all tools from the tool API and display them here
   (if (= panel-name "tool-store")
     {:db (assoc db :debug-panel panel-name)
      ::fx/http {:uri "/api/tools/all"
                 :method :get
                 :on-success [::tool-load-success]
                 :on-denied [::tool-load-fail]}}
     {:db (assoc db :debug-panel panel-name)})))

(reg-event-fx ::tool-load-success
              (fn [{db :db} [_ tools]]
                {:db (assoc-in db [:developer :tools] tools)}))
