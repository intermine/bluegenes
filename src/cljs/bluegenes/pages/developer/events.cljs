(ns bluegenes.pages.developer.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [bluegenes.components.tools.events :as tools]))

(reg-event-fx
 ::panel
 (fn [{db :db} [_ panel-name]]
   (let [effects {:db (assoc db :debug-panel panel-name)}]
     (case panel-name
       "main"
       (assoc effects :dispatch [::tools/fetch-tool-path])
       effects))))
