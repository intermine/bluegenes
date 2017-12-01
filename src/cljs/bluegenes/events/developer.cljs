(ns bluegenes.events.developer
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [subscribe reg-event-db reg-event-db reg-event-fx]]
            [cljs.core.async :refer [<!]]))

(reg-event-db
  ::panel
  (fn [db [_ panel-name]]
    (assoc db :debug-panel panel-name)))
