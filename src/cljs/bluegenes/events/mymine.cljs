(ns bluegenes.events.mymine
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx]]
            [cljs.core.async :refer [<!]]
            [imcljs.send :as send]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [bluegenes.effects :as fx]
            [clojure.string :as s]
            [cljs-uuid-utils.core :refer [make-random-uuid]]))

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
  ::set-action-target
  (fn [db [_ path-vec]]
    (assoc-in db [:mymine :action-target] path-vec)))

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

    (let [action-target (get-in db [:mymine :action-target])]
      (update-in db [:mymine :tree] update-in action-target assoc key value :editing? false))))

(reg-event-db
  ::new-folder
  (fn [db [_ location-trail name]]
    (let [action-target (not-empty (get-in db [:mymine :action-target]))
          uuid          (make-random-uuid)]
      (if-not action-target
        (-> db (assoc-in [:mymine :tree uuid] {:label name :file-type :folder}))
        (-> db
            ; Assocation the new folder into the tree
            (update-in [:mymine :tree] update-in (or location-trail []) update :children assoc uuid {:label name :file-type :folder})
            ; Open the parent
            (update-in [:mymine :tree] update-in (or location-trail []) assoc :open true)
            ;
            (assoc-in [:mymine :focus] (if (nil? location-trail) [uuid] (conj (vec location-trail) :children uuid)))
            )))))


(defn parent-container [path]
  (if (= (last (butlast path)) :children)
    (recur (butlast path))
    (butlast path)))


(reg-event-db
  ::delete-folder
  (fn [db [_ location-trail name]]
    (let [action-target (not-empty (get-in db [:mymine :action-target]))]
      (-> db
         ; Dissoc the folder
         (update-in [:mymine :tree] dissoc-in action-target)
         (assoc-in [:mymine :focus] (parent-container action-target))))))

(reg-event-db
  ::toggle-selected
  (fn [db [_ location-trail options {:keys [id file-type] :as selected}]]
    (let [db (assoc-in db [:mymine :details] (select-keys selected [:id :file-type]))]
      (cond
        (:force? options) (assoc-in db [:mymine :selected] #{location-trail})
        (:single? options) (if (in? (get-in db [:mymine :selected]) location-trail)
                             (assoc-in db [:mymine :selected] #{})
                             (assoc-in db [:mymine :selected] #{location-trail}))))
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
  (fn [db [_ file-details]]
    (update db :mymine assoc
            :dragging file-details
            :dragging-node file-details)))

; TODO
; We're freshing everything because currently the send/copy-list end point doesn't return a list ID
; This can be greatly reduced in the future (see to ;; END TODO below)
(reg-event-fx
  ::fetch-one-list-success
  (fn [{db :db} [_ trail {:keys [id name] :as list-details}]]
    (cond-> {:dispatch [:assets/fetch-lists]}
            (and trail (not (keyword? (first trail)))) (assoc :db (update-in db [:mymine :tree]
                                                                             update-in (parent-container trail)
                                                                             update :children assoc
                                                                             id (assoc list-details
                                                                                  :file-type :list
                                                                                  :label name))))))

(reg-event-fx
  ::fetch-one-list
  (fn [{db :db} [_ trail {list-name :listName}]]
    (let [service (get-in db [:mines (get db :current-mine) :service])]
      {:im-chan {:chan (fetch/one-list service list-name)
                 :on-success [::fetch-one-list-success trail]
                 :on-failure [::copy-failure]}})))

(reg-event-fx
  ::copy-success
  (fn [{db :db} [_ trail response]]
    {:dispatch-n [
                  [::clear-checked]
                  [::fetch-one-list trail response]]}))

(reg-event-fx
  ::copy-n
  (fn [{db :db}]
    (let [ids           (get-in db [:mymine :checked])
          lists         (get-in db [:assets :lists (get db :current-mine)])
          names-to-copy (map :name (filter (comp ids :id) lists))
          focus         (or (get-in db [:mymine :focus]) [:unsorted])]
      ; Now automatically increment the names of the list (since we're copying many)
      (let [evts (reduce (fn [total next]
                           (if-let [previous (first (not-empty (filter #(s/starts-with? % (str next "_")) (map :name lists))))]
                             (do
                               (conj total [::copy-focus focus next (str previous "_" (-> previous last (js/parseInt) inc))]))
                             (do
                               (conj total [::copy-focus focus next (str next "_1")])))) [] names-to-copy)]
        {:db db
         :dispatch-n evts}))))

(reg-event-fx
  ::copy-focus
  (fn [{db :db} [_ trail old-list-name new-list-name]]
    ;[on-success on-failure response-format chan params]
    (let [service          (get-in db [:mines (get db :current-mine) :service])
          target-file      (get-in db [:mymine :menu-file-details])
          lists            (get-in db [:assets :lists (get db :current-mine)])
          target-list-name (->> lists (filter (fn [l] (= (:id target-file) (:id l)))) first :name)
          location         (butlast (:trail target-file))]
      {:im-chan {:chan (save/im-list-copy service old-list-name new-list-name)
                 :on-success [::copy-success location]
                 :on-failure [::copy-failure]}})))

(reg-event-fx
  ::copy
  (fn [{db :db} [_ trail old-list-name new-list-name]]
    ;[on-success on-failure response-format chan params]
    (let [service          (get-in db [:mines (get db :current-mine) :service])
          target-file      (get-in db [:mymine :menu-file-details])
          lists            (get-in db [:assets :lists (get db :current-mine)])
          target-list-name (->> lists (filter (fn [l] (= (:id target-file) (:id l)))) first :name)
          location         (let [t (butlast (:trail target-file))]
                             (if (= t '(:public)) nil t))]
      {:im-chan {:chan (save/im-list-copy service old-list-name new-list-name)
                 :on-success [::copy-success location]
                 :on-failure [::copy-failure]}})))

;;;; END TODO

(reg-event-fx
  ::delete-success
  (fn [{db :db} [_ trail response]]
    {:dispatch [:assets/fetch-lists trail response]}))


(reg-event-fx
  ::delete
  (fn [{db :db} [_ trail list-name]]
    ;[on-success on-failure response-format chan params]
    (let [service (get-in db [:mines (get db :current-mine) :service])]
      {:im-chan {:chan (send/delete-list service list-name)
                 :on-success [::delete-success trail]
                 :on-failure [::delete-failure]}})))

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
          :else (do
                  {:db (update-in db [:mymine :tree]
                                  #(-> %
                                       ; Remove this node from the tree
                                       (dissoc-in (:trail dragging-node))
                                       ; Re-associate to the new location
                                       (update-in drop-parent-folder assoc-in [:children (or (:id dragging-node) (last (:trail dragging-node)))] (select-keys dragging-node [:file-type :id]))))
                   ; Reselect the item at its new location
                   :dispatch-n [[::drag-end]]}))))))
;[::toggle-selected (concat drop-parent-folder [:children (last dragging)]) {:force? true}]



(reg-event-db
  ::op-select-item
  (fn [db [_ id]]
    (update-in db [:mymine :list-operations :selected]
               (fn [s]
                 (if (some? (some #{id} s)) ; If the item id has already been selected...
                   (remove #{id} s) ; ... then remove it
                   (conj s id)))))) ; ... otherwise add it

(defn toggle-set [coll k]
  (let [s (set coll)]
    (if (contains? s k)
      (remove #{k} s)
      (conj s k))))

(reg-event-db
  ::toggle-checked
  (fn [db [_ id]]
    (update-in db [:mymine :checked] toggle-set id)))

(reg-event-fx
  ::lo-combine
  (fn [{db :db} [_ list-name]]
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-union service list-name selected-lists)
                   :on-success [::lo-success]
                   :on-failure [::lo-success]}}))))

(reg-event-fx
  ::lo-intersect
  (fn [{db :db} [_ list-name]]
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-intersect service list-name selected-lists)
                   :on-success [::lo-success]
                   :on-failure [::lo-success]}}))))

(reg-event-db
  ::clear-checked
  (fn [db]
    (assoc-in db [:mymine :checked] #{})))

(reg-event-db
  ::set-menu-target
  (fn [db [_ file-details]]
    (update-in db [:mymine] assoc
               :menu-file-details file-details)))

(reg-event-fx
  ::lo-success
  (fn [{db :db} [_ m]]
    (let [focus (get-in db [:mymine :focus])]
      {:dispatch-n [[::clear-checked]
                    [:assets/fetch-lists]]})))