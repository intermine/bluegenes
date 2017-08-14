(ns bluegenes.events.mymine
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
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

(defn in? [haystack needle]
  (some? (some #{needle} haystack)))

(reg-event-db
  ::toggle-folder-open
  (fn [db [_ location-trail]]
    (update-in db [:mymine :tree] update-in location-trail update :open not)))

(reg-event-db
  ::set-focus
  (fn [db [_ location-trail]]
    (assoc-in db [:mymine :focus] location-trail)))

(reg-event-db
  ::update-value
  (fn [db [_ location-trail key value]]
    (update-in db [:mymine :tree] update-in location-trail assoc key value :editing? false)))

(reg-event-db
  ::toggle-selected
  (fn [db [_ location-trail options]]
    (cond
      (:force? options) (assoc-in db [:mymine :selected] #{location-trail})
      (:single? options) (if (in? (get-in db [:mymine :selected]) location-trail)
                           (assoc-in db [:mymine :selected] #{})
                           (assoc-in db [:mymine :selected] #{location-trail})))
    #_(if (:reset? options)
        (assoc-in db [:mymine :selected] #{[location-trail]})
        (update-in db [:mymine :selected] in-or-out location-trail))
    #_(update-in db [:mymine :selected] in-or-out location-trail)))

(reg-event-db
  ::toggle-sort
  (fn [db [_ key type direction]]
    (if (= key (get-in db [:mymine :sort-by :key]))
      ; If the key being toggled is the key being set then change the sort direction
      (-> db (update-in [:mymine :sort-by :asc?] not) (assoc-in [:mymine :sort-by :type] type))
      ; Otherwise it's a new column so set the key and a default sort direction
      (assoc-in db [:mymine :sort-by] {:key key :type type :asc? true}))))

(reg-event-db
  ::drag-start
  (fn [db [_ trail]]
    (update db :mymine assoc :dragging trail)))

(reg-event-db
  ::drag-over
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] trail)))

(reg-event-db
  ::drag-end
  (fn [db [_ trail]]
    db))

(reg-event-db
  ::set-context-menu-target
  (fn [db [_ trail]]
    (assoc-in db [:mymine :context-target] trail)))

(defn parent-folder
  "Find the location of the parent folder for a given path"
  ([tree path]
   (parent-folder tree path []))
  ([tree [head children-key & remaining] trail]
    ; If the root of the tree if a folder
   (if (-> (get tree head) :file-type (= :folder))
     ; Recur with subtree, remaining path, and keys to location
     (recur (get-in tree [head children-key]) remaining (vec (conj trail head)))
     ; No more folders in path, return the trail through the tree with :children in between
     (interpose :children trail))))

(reg-event-fx
  ::drop
  (fn [{db :db} [_ trail]]
    (let [{:keys [dragging dragging-over]} (:mymine db)]
      (let [tree               (get-in db [:mymine :tree])
            drop-parent-folder (parent-folder tree dragging-over)
            drag-type          (:file-type (get-in tree dragging))
            drop-type          (:file-type (get-in tree dragging-over))]
        ; Don't do anything if we're moving something into the same folder
        (cond
          (= dragging dragging-over) {:db db} ; File was moved onto itself. Ignore.
          ;(and (= :folder drag-type) (= :folder drag-type)) db
          :else {:db (update-in db [:mymine :tree]
                                #(-> %
                                     ; Remove this node from the tree
                                     (update-in (butlast dragging) dissoc (last dragging))
                                     ; Re-associate to the new location
                                     (update-in drop-parent-folder assoc-in [:children (last dragging)] (get-in % dragging))))
                 ; Reselect the item at its new location
                 :dispatch [::toggle-selected (concat drop-parent-folder [:children (last dragging)]) {:force? true}]})))))
