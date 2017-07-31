(ns bluegenes.events.mymine
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(defn get-at [pos-vec data]
  (get-in data (conj (interpose :children pos-vec) :children)))

;(defn toggle-open [pos-vec]
;  (swap! mock-tree update-in
;         (conj (interpose :children pos-vec) :children) update :open not))

(reg-event-db
  ::toggle-folder-open
  (fn [db [_ location-trail]]
    (update db :mymine
            ; Interpose :children in between each item in the location trail
            ; so we can navigate to it in our MyMine tree. Also conj :children
            ; onto the front as we're starting from the root
            update-in (conj (interpose :children location-trail))
            ; Toggle the child's open state
            update :open not)))