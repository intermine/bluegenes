(ns bluegenes.events.auth
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]))

(reg-event-fx
  ::login
  (fn [{db :db} [_ {:keys [username password]}]]
    {:db db}))