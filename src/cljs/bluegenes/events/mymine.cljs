(ns bluegenes.events.mymine
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs-http.client :refer [get post]]
            [cljs.core.async :refer [<!]]))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn in-or-out [haystack needle]
  (if (some? (some #{needle} haystack))
    (remove #{needle} haystack)
    (conj haystack needle)))

(reg-event-db
  ::toggle-folder-open
  (fn [db [_ location-trail]]
    (update-in db [:mymine :tree] update-in location-trail update :open not)))

(reg-event-db
  ::toggle-selected
  (fn [db [_ location-trail index options]]
    (if (:reset? options)
      (assoc-in db [:mymine :selected] #{[location-trail]})
      (update-in db [:mymine :selected] in-or-out location-trail))))

(reg-event-db
  ::toggle-sort
  (fn [db [_ key]]
    (if (= key (get-in db [:mymine :sort-by :key]))
      ; If the key being toggled is the key being set then change the sort direction
      (update-in db [:mymine :sort-by :asc?] not)
      ; Otherwise it's a new column so set the key and a default sort direction
      (assoc-in db [:mymine :sort-by] {:key key :asc? true}))))

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
      (let [full-drag-path (vec (concat [:mymine :tree] dragging))
            full-drop-path (vec (concat [:mymine :tree] dragging-over))]
        (-> db
            (update-in (butlast full-drag-path) dissoc (last full-drag-path))
            (assoc-in
              (concat full-drop-path [:children (last full-drag-path)])
              (get-in db full-drag-path)))))))
