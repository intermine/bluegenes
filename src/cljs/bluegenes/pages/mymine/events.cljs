(ns bluegenes.pages.mymine.events
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx subscribe]]
            [imcljs.send :as send]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [clojure.string :as s]
            [clojure.set :as core.set]
            [oops.core :refer [ocall]]
            [bluegenes.pages.mymine.views.organize :as organize]))

(reg-event-db
 ::set-action-target
 (fn [db [_ path-vec]]
   (assoc-in db [:mymine :action-target] path-vec)))

(defn parent-container [path]
  (if (= (last (butlast path)) :children)
    (recur (butlast path))
    (butlast path)))

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
 (fn [{db :db} [_ trail parent-id {:keys [id name] :as list-details}]]
   (cond-> {:dispatch [:assets/fetch-lists]}
     (and trail (not (keyword? (first trail))))
     (assoc :db (update-in db [:mymine :tree]
                           update-in (parent-container trail)
                           update :children assoc
                           id (assoc list-details
                                     :file-type :list
                                     :label name))))))

(reg-event-fx
 ::fetch-one-list
 (fn [{db :db} [_ trail {list-name :listName} parent-id]]
   (js/console.log "fetching one list" list-name)
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (fetch/one-list service list-name)
                :on-success [::fetch-one-list-success trail parent-id]
                :on-failure [::copy-failure]}})))

(reg-event-fx
 ::copy-success
 (fn [{db :db} [_ response]]
   (js/console.log "copy success" response)
   {:dispatch-n [[::clear-checked]
                 [::fetch-one-list response]]}))

(reg-event-fx
 ::copy-n
 (fn [{db :db}]
   (let [ids (get-in db [:mymine :checked])
         lists (get-in db [:assets :lists (get db :current-mine)])
         names-to-copy (map :name (filter (comp ids :id) lists))
         focus (or (get-in db [:mymine :focus]) [:unsorted])
         parent-id (get-in db [:mymine :cursor :entry-id])]
      ; Now automatically increment the names of the list (since we're copying many)
     (let [evts (reduce (fn [total next]
                          (if-let [previous (first (not-empty (filter #(s/starts-with? % (str next "_")) (map :name lists))))]
                            (conj total
                                  [::copy-focus focus next (str previous "_" (-> previous last (js/parseInt) inc)) parent-id])
                            (conj total
                                  [::copy-focus focus next (str next "_1") parent-id])))
                        []
                        names-to-copy)]
       {:db db
        :dispatch-n evts}))))

(reg-event-fx
 ::copy-focus
 (fn [{db :db} [_ trail old-list-name new-list-name parent-id]]
    ;[on-success on-failure response-format chan params]
   (let [service (get-in db [:mines (get db :current-mine) :service])
         target-file (get-in db [:mymine :menu-file-details])
         lists (get-in db [:assets :lists (get db :current-mine)])
         target-list-name (->> lists (filter (fn [l] (= (:id target-file) (:id l)))) first :name)
         location (butlast (:trail target-file))]
     {:im-chan {:chan (save/im-list-copy service old-list-name new-list-name)
                :on-success [::copy-success location parent-id]
                :on-failure [::copy-failure]}})))

(reg-event-fx
 ::delete-lists-success
 (fn [{db :db} [_ returned]]
   {:dispatch [::lo-success]}))

(reg-event-fx
 ::delete-lists
 (fn [{db :db} [_]]
   (let [lists (get-in db [:assets :lists (get db :current-mine)])
         checked (get-in db [:mymine :checked])
         selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))]
     (let [service (get-in db [:mines (get db :current-mine) :service])]
       {:im-chan {:chan (save/im-list-delete service selected-lists)
                  :on-success [::delete-lists-success]
                  :on-failure [::delete-lists-success]}}))))

(reg-event-fx
 ::copy
 (fn [{db :db} [_ old-list-name new-list-name]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (save/im-list-copy service old-list-name new-list-name)
                :on-success [:assets/fetch-lists]
                :on-failure [::copy-failure]}})))

;;;; END TODO

(reg-event-fx
 ::update-tags
 (fn [{db :db} [_ new-tags]]
   (let [old-tags (->> (get-in db [:assets :lists (:current-mine db)])
                       (reduce (fn [m {:keys [tags title]}]
                                 (assoc m title (first (organize/extract-path-tag tags))))
                               {}))
         ;; Compute the changes in the path tags for the lists, and generate
         ;; the appropriate remove-tag and add-tag events.
         tag-events (->> (core.set/difference (set new-tags) (set old-tags))
                         (map (fn [[title new-tag]]
                                (let [old-tag (get old-tags title)]
                                  [(when old-tag [::remove-tag title old-tag])
                                   (when new-tag [::add-tag title new-tag])])))
                         (apply concat)
                         (filter some?))]
     (if (empty? tag-events)
       (do (ocall (js/$ "#myMineOrganize") :modal "hide")
           {:db (update-in db [:mymine :modals :organize] dissoc :error)})
       {:dispatch-n tag-events
        :db (assoc-in db [:mymine :pending-tag-operations] (set tag-events))}))))

(reg-event-fx
 ::add-tag
 (fn [{db :db} [_ list-name tags :as evt]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (save/im-list-add-tag service list-name tags)
                :on-success [::tag-success evt]
                :on-failure [::tag-failure evt]}})))
(reg-event-fx
 ::remove-tag
 (fn [{db :db} [_ list-name tags :as evt]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (save/im-list-remove-tag service list-name tags)
                :on-success [::tag-success evt]
                :on-failure [::tag-failure evt]}})))

(reg-event-fx
 ::tag-success
 (fn [{db :db} [_ evt]]
   (let [operations (disj (get-in db [:mymine :pending-tag-operations]) evt)
         db' (assoc-in db [:mymine :pending-tag-operations] operations)]
     (if (empty? operations)
       (do (ocall (js/$ "#myMineOrganize") :modal "hide")
           {:db (update-in db' [:mymine :modals :organize] dissoc :error)
            :dispatch [:assets/fetch-lists]})
       {:db db'}))))

(reg-event-db
 ::tag-failure
 (fn [db [_ _evt]]
   (-> db
       (update :mymine dissoc :pending-tag-operations)
       (assoc-in [:mymine :modals :organize :error] "Failed to save changes to folder hierarchy. Please check your network connection and try again."))))

(reg-event-fx
 ::rename-list
 (fn [{db :db} [_ old-list-name new-list-name]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (save/im-list-rename service old-list-name new-list-name)
                :on-success [:assets/fetch-lists]
                :on-failure [::copy-failure]}})))

(reg-event-fx
 ::delete
 (fn [{db :db} [_ list-name]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     {:im-chan {:chan (send/delete-list service list-name)
                :on-success [:assets/fetch-lists]
                :on-failure [::delete-failure]}})))

(reg-event-db
 ::set-context-menu-target
 (fn [db [_ entity]]
   (update-in db [:mymine] assoc :context-menu-target entity)))

(defn toggle-set [coll k]
  (let [s (set coll)]
    (if (contains? s k)
      (set (remove #{k} s))
      (set (conj s k)))))

(reg-event-db
 ::toggle-checked
 (fn [db [_ id]]
   (update-in db [:mymine :checked] toggle-set id)))

(reg-event-db
 ::set-modal
 (fn [db [_ modal-kw]]
   (let [a (get-in db [:mymine :checked])
         lists (subscribe [:lists/filtered-lists])
          ;;sort lists and reverse so we can subtract the smallest list
          ;; from the larger lists by default. Users can change the order
          ;; if they need to, but this seems a sane default.
         sorted-lists (reverse (sort-by :size (filter (fn [l] (some #{(:id l)} a)) @lists)))]
     (update-in db [:mymine] assoc
                :modal modal-kw
                :suggested-state [(butlast sorted-lists) [(last sorted-lists)]]))))

(reg-event-fx
 ::lo-combine
 (fn [{db :db} [_ list-name]]
   (let [lists (get-in db [:assets :lists (get db :current-mine)])
         checked (get-in db [:mymine :checked])
         selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
         parent-id (get-in db [:mymine :cursor :entry-id])]
     (let [service (get-in db [:mines (get db :current-mine) :service])]
       {:im-chan {:chan (save/im-list-union service list-name selected-lists)
                  :on-success [::lo-success parent-id]
                  :on-failure [::lo-success]}}))))

(reg-event-fx
 ::lo-intersect
 (fn [{db :db} [_ list-name]]
   (let [lists (get-in db [:assets :lists (get db :current-mine)])
         checked (get-in db [:mymine :checked])
         selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
         parent-id (get-in db [:mymine :cursor :entry-id])]
     (let [service (get-in db [:mines (get db :current-mine) :service])]
       {:im-chan {:chan (save/im-list-intersect service list-name selected-lists)
                  :on-success [::lo-success parent-id]
                  :on-failure [::lo-success]}}))))

(reg-event-fx
 ::lo-difference
 (fn [{db :db} [_ list-name]]
   (let [lists (get-in db [:assets :lists (get db :current-mine)])
         checked (get-in db [:mymine :checked])
         selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
         parent-id (get-in db [:mymine :cursor :entry-id])]
     (let [service (get-in db [:mines (get db :current-mine) :service])]
       {:im-chan {:chan (save/im-list-difference service list-name selected-lists)
                  :on-success [::lo-success parent-id]
                  :on-failure [::lo-success]}}))))

(reg-event-fx
 ::lo-subtract
 (fn [{db :db} [_ list-name]]
   (let [lists (get-in db [:assets :lists (get db :current-mine)])
         [lists-a lists-b] (get-in db [:mymine :suggested-state])
         selected-lists-a (map :name lists-a)
         selected-lists-b (map :name lists-b)
         parent-id (get-in db [:mymine :cursor :entry-id])]

     (let [service (get-in db [:mines (get db :current-mine) :service])]
       {:im-chan {:chan (save/im-list-subtraction service list-name selected-lists-a selected-lists-b)
                  :on-success [::lo-success parent-id]
                  :on-failure [::lo-success]}}))))

(reg-event-db
 ::lo-reverse-order
 (fn [db [_]]
   (update-in db [:mymine :suggested-state] (comp vec reverse))))

(reg-event-db
 ::lo-move-bucket
 (fn [db [_ from-pos id]]
   (-> db
       (update-in [:mymine :suggested-state from-pos] #(set (remove #{id} %)))
       (update-in [:mymine :suggested-state (if (= from-pos 0) 1 0)] #(set (conj % id))))))

(reg-event-db
 ::clear-checked
 (fn [db]
   (assoc-in db [:mymine :checked] #{})))

(reg-event-fx
 ::lo-success
 (fn [{db :db} [_ parent-id m]]
   (let [focus (get-in db [:mymine :focus])]
     {:dispatch-n [[::clear-checked]
                   [:assets/fetch-lists]]})))

(reg-event-db ::dragging
              (fn [db [_ tag]]
                (update-in db [:mymine :drag] assoc :dragging tag :dragging? true)))

(reg-event-db ::dragging?
              (fn [db [_ value]]
                (assoc-in db [:mymine :drag :dragging?] value)))

(reg-event-db ::dragging-over
              (fn [db [_ tag]]
                (assoc-in db [:mymine :drag :dragging-over] tag)))

(reg-event-fx ::dropping-on
              (fn [{db :db} [_ tag]]
                (let [{dragging-id :entry-id :as dragging} (get-in db [:mymine :drag :dragging])
                      {dropping-id :entry-id :as dropping} (get-in db [:mymine :drag :dragging-over])
                      hierarchy (get-in db [:mymine :hierarchy])
                      current-mine (get-in db [:current-mine])]
                  (let [noop {:db (assoc-in db [:mymine :drag] nil)}]

                    (cond
                      (isa? hierarchy
                            (keyword "tag" (:entry-id dropping))
                            (keyword "tag" (:entry-id dragging))) noop
                      (not= "tag" (:im-obj-type dropping)) noop
                      :else noop)))))
