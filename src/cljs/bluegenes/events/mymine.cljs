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

(defn snowball
  "Makes a growing collection like the following:
  (snowball [1 2 3 4 5])
  => [[1] [1 2] [1 2 3] [1 2 3 4] [1 2 3 4 5]]"
  [coll]
  (if (seqable? coll)
    (reduce (fn [total next] (conj total (vec (conj (last total) next)))) [] coll)
    [coll]))

(reg-event-db
  ::set-focus
  (fn [db [_ location-trail expand?]]
    (cond-> db
            ; Set the focus to the current location in the tree
            true (assoc-in [:mymine :focus] location-trail)
            ; If we're told to expand then make sure all parent folders are open
            expand? (update-in [:mymine :tree]
                               (fn [tree]
                                 (reduce (fn [total next]
                                           (if (not= (last next) :children)
                                             (update-in total next assoc :open true)
                                             total))
                                         tree (butlast (snowball location-trail))))))))

(reg-event-db
  ::update-value
  (fn [db [_ location-trail key value]]
    (update-in db [:mymine :tree] update-in location-trail assoc key value :editing? false)))

(reg-event-db
  ::new-folder
  (fn [db [_ location-trail name]]
    (-> db
        ; Assocation the new folder into the tree
        (update-in [:mymine :tree] update-in location-trail update :children assoc name {:label name :file-type :folder})
        ; Open the parent
        (update-in [:mymine :tree] update-in location-trail assoc :open true))))

(defn parent-container [path]
  (if (= (last (butlast path)) :children)
    (recur (butlast path))
    (butlast path)))


(reg-event-db
  ::delete-folder
  (fn [db [_ location-trail name]]
    (-> db
        ; Dissoc the folder
        (update-in [:mymine :tree] dissoc-in location-trail)
        (assoc-in [:mymine :focus] (parent-container location-trail)))))

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
  (fn [db [_ trail node]]
    (update db :mymine assoc
            :dragging trail
            :dragging-node node)))

(reg-event-db
  ::drag-over
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] trail)))

(reg-event-db
  ::drag-end
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] nil)))

(reg-event-db
  ::set-context-menu-target
  (fn [db [_ trail node]]
    (update-in db [:mymine] assoc
               :context-target trail
               :context-node node)))

(reg-event-fx
  ::drop
  (fn [{db :db} [_ trail]]
    (let [{:keys [dragging dragging-over dragging-node]} (:mymine db)]
      (let [tree               (get-in db [:mymine :tree])
            drop-parent-folder (parent-folder tree dragging-over)
            drag-type          (:file-type (get-in tree dragging))
            drop-type          (:file-type (get-in tree dragging-over))]
        ; Don't do anything if we're moving something into the same folder
        (cond
          (= dragging dragging-over) {:db db :dispatch [::drag-end]} ; File was moved onto itself. Ignore.
          ;(and (= :folder drag-type) (= :folder drag-type)) db
          :else {:db (update-in db [:mymine :tree]
                                #(-> %
                                     ; Remove this node from the tree
                                     (dissoc-in (:trail dragging-node))
                                     ; Re-associate to the new location
                                     (update-in drop-parent-folder assoc-in [:children (last (:trail dragging-node))] dragging-node)))
                 ; Reselect the item at its new location
                 :dispatch-n [[::drag-end]
                              ;[::toggle-selected (concat drop-parent-folder [:children (last dragging)]) {:force? true}]
                              ]})))))
