(ns bluegenes.events.mymine
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(reg-event-db
  ::toggle-folder-open
  (fn [db [_ location-trail]]
    (update-in db [:mymine :tree]
               ; Interpose :children in between each item in the location trail
               ; so we can navigate to it in our MyMine tree. Also conj :children
               ; onto the front as we're starting from the root
               update-in (conj (interpose :children location-trail))
               ; Toggle the child's open state
               update :open not)))

(reg-event-db
  ::drag-start
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging] trail)))

(reg-event-db
  ::drag-over
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] trail)))

(reg-event-db
  ::drag-end
  (fn [db [_ trail]]
    db))

(reg-event-db
  ::drop
  (fn [db [_ trail]]
    (let [{:keys [dragging dragging-over]} (:mymine db)]
      db)))
