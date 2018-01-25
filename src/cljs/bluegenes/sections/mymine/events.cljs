(ns bluegenes.sections.mymine.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-db reg-event-fx reg-fx subscribe]]
            [cljs.core.async :refer [<!]]
            [imcljs.send :as send]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [bluegenes.effects :as fx]
            [clojure.string :as s]
            [bluegenes.sections.mymine.subs :as subs]
            [oops.core :refer [ocall]]
            [clojure.walk :as walk]
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
          uuid          (keyword (str "tag-" (str (make-random-uuid))))]
      (if-not action-target
        (-> db (assoc-in [:mymine :tree uuid] {:label name :file-type :folder}))
        (-> db
            ; Assocation the new folder into the tree
            (update-in [:mymine :tree] update-in (or location-trail []) update :children assoc uuid {:label name :file-type :folder})
            ; Open the parent
            (update-in [:mymine :tree] update-in (or location-trail []) assoc :open true)
            ;
            (assoc-in [:mymine :focus] (if (nil? location-trail) [uuid] (conj (vec location-trail) :children uuid))))))))



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
  (fn [{db :db} [_ trail parent-id {:keys [id name] :as list-details}]]
    (cond-> {:dispatch [:assets/fetch-lists]}
            (and trail (not (keyword? (first trail)))) (assoc :db (update-in db [:mymine :tree]
                                                                             update-in (parent-container trail)
                                                                             update :children assoc
                                                                             id (assoc list-details
                                                                                  :file-type :list
                                                                                  :label name)))
            parent-id (assoc ::fx/http {:method :post
                                        :transit-params {:im-obj-type "list"
                                                         :im-obj-id id
                                                         :parent-id parent-id}
                                        :on-success [::success-store-tag]
                                        :uri "/api/mymine/entries"}))))

(reg-event-fx
  ::fetch-one-list
  (fn [{db :db} [_ trail {list-name :listName} parent-id]]
    (let [service (get-in db [:mines (get db :current-mine) :service])]
      {:im-chan {:chan (fetch/one-list service list-name)
                 :on-success [::fetch-one-list-success trail parent-id]
                 :on-failure [::copy-failure]}})))

(reg-event-fx
  ::copy-success
  (fn [{db :db} [_ trail parent-id response]]
    {:dispatch-n [
                  [::clear-checked]
                  [::fetch-one-list trail response parent-id]]}))

(reg-event-fx
  ::copy-n
  (fn [{db :db}]
    (let [ids           (get-in db [:mymine :checked])
          lists         (get-in db [:assets :lists (get db :current-mine)])
          names-to-copy (map :name (filter (comp ids :id) lists))
          focus         (or (get-in db [:mymine :focus]) [:unsorted])
          parent-id     (get-in db [:mymine :cursor :entry-id])]
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
    (let [service          (get-in db [:mines (get db :current-mine) :service])
          target-file      (get-in db [:mymine :menu-file-details])
          lists            (get-in db [:assets :lists (get db :current-mine)])
          target-list-name (->> lists (filter (fn [l] (= (:id target-file) (:id l)))) first :name)
          location         (butlast (:trail target-file))]
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
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-delete service selected-lists)
                   :on-success [::delete-lists-success]
                   :on-failure [::delete-lists-success]}}))))

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
  ::rename-list
  (fn [{db :db} [_ new-list-name]]
    (let [service (get-in db [:mines (get db :current-mine) :service])
          _       (js/console.log "DB" db)
          id      (last (get-in db [:mymine :action-target]))
          _       (js/console.log "ID" id)
          {old-list-name :name} @(subscribe [::subs/one-list id])]
      (js/console.log "RENAMING" old-list-name new-list-name)
      {:im-chan {:chan (save/im-list-rename service old-list-name new-list-name)
                 :on-success [:assets/fetch-lists]
                 :on-failure [::copy-failure]}})))

(reg-event-fx
  ::delete
  (fn [{db :db} [_ trail list-name]]
    ;[on-success on-failure response-format chan params]
    (let [service (get-in db [:mines (get db :current-mine) :service])]
      {:im-chan {:chan (send/delete-list service list-name)
                 :on-success [::delete-success trail]
                 :on-failure [::delete-failure]}})))

(reg-event-db
  ::drag-over-old
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] trail)))

(reg-event-db
  ::drag-end
  (fn [db [_ trail]]
    (assoc-in db [:mymine :dragging-over] nil)))

(reg-event-db
  ::set-context-menu-target
  (fn [db [_ entity]]
    (update-in db [:mymine] assoc :context-menu-target entity)))

(reg-event-fx
  ::drop
  (fn [{db :db} [_ trail]]
    (let [{:keys [dragging dragging-over dragging-node]} (:mymine db)]
      (let [tree               (get-in db [:mymine :tree])
            drop-parent-folder (parent-folder tree dragging-over)
            drag-type          (:file-type (get-in tree dragging))
            drop-type          (:file-type (get-in tree dragging-over))
            dragging-id        (keyword (str (name (:file-type dragging-node)) "-" (:id dragging-node)))]
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
                                       (update-in drop-parent-folder assoc-in [:children dragging-id] (select-keys dragging-node [:file-type :id]))))
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
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
          parent-id      (get-in db [:mymine :cursor :entry-id])]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-union service list-name selected-lists)
                   :on-success [::lo-success parent-id]
                   :on-failure [::lo-success]}}))))

(reg-event-fx
  ::lo-intersect
  (fn [{db :db} [_ list-name]]
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
          parent-id      (get-in db [:mymine :cursor :entry-id])]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-intersect service list-name selected-lists)
                   :on-success [::lo-success parent-id]
                   :on-failure [::lo-success]}}))))

(reg-event-fx
  ::lo-difference
  (fn [{db :db} [_ list-name]]
    (let [lists          (get-in db [:assets :lists (get db :current-mine)])
          checked        (get-in db [:mymine :checked])
          selected-lists (map :name (filter (fn [l] (some #{(:id l)} checked)) lists))
          parent-id      (get-in db [:mymine :cursor :entry-id])]
      (let [service (get-in db [:mines (get db :current-mine) :service])]
        {:im-chan {:chan (save/im-list-difference service list-name selected-lists)
                   :on-success [::lo-success parent-id]
                   :on-failure [::lo-success]}}))))

(reg-event-fx
  ::lo-subtract
  (fn [{db :db} [_ list-name]]
    (let [lists            (get-in db [:assets :lists (get db :current-mine)])
          [lists-a lists-b] (get-in db [:mymine :suggested-state])
          selected-lists-a (map :name lists-a)
          selected-lists-b (map :name lists-b)
          parent-id        (get-in db [:mymine :cursor :entry-id])]

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

(reg-event-db
  ::set-menu-target
  (fn [db [_ file-details]]
    (update-in db [:mymine] assoc
               :menu-file-details file-details)))

(reg-event-fx
  ::lo-success
  (fn [{db :db} [_ parent-id m]]
    (let [focus (get-in db [:mymine :focus])]
      {:dispatch-n [[::clear-checked]
                    [:assets/fetch-lists]]
       ::fx/http {:method :post
                  :transit-params {:im-obj-type "list"
                                   :im-obj-id (:listId m)
                                   :parent-id parent-id}
                  :on-success [::success-store-tag]
                  :uri "/api/mymine/entries"}})))





;;;;;;;;;;  Tag tree operations

(defn dissoc-nested-keys
  "Recursively dissociate keys from a deep map
  (dissoc-nested-keys
      {:a {:x {:b {:c {}}}}, :d {:e {:x {:g {}}}, :h {}}}
      #{:x :h})
  => {:a {}, :d {:e {}}}"
  [m key-col]
  (walk/postwalk #(if (map? %) (apply dissoc % key-col) %) m))

(reg-event-db
  ::remove-ids-from-tree
  (fn [db [_ list-ids]]
    (update-in db [:mymine :tree] dissoc-nested-keys list-ids)))


;;;;;;;;;;; IO Operations

(reg-event-fx ::store-tag []
              (fn [{db :db} [_ label]]
                (let [context-menu-target (get-in db [:mymine :context-menu-target])
                      mine-id             (get-in db [:current-mine])]
                  {:db db
                   ::fx/http {:method :post
                              :transit-params {:im-obj-type "tag"
                                               :parent-id (:entry-id context-menu-target)
                                               :label label
                                               :mine (name mine-id)
                                               :open? true}
                              :on-success [::success-store-tag]
                              :uri "/api/mymine/entries"}})))




(reg-fx ::rederive-tags
        (fn [tags]

          (println "rederiving" (count tags))

          ; Clear all known derivations
          (doseq [{:keys [entry-id parent-id]} tags]
            (doseq [ancestor (ancestors (keyword "tag" entry-id))]
              (underive (keyword "tag" entry-id) ancestor)))

          ; Rederive tags
          (doseq [{:keys [entry-id parent-id]} tags]
            (when (and entry-id parent-id)
              (derive
                (keyword "tag" entry-id)
                (keyword "tag" parent-id))))))

(reg-event-db ::rederive
              (fn [db [_]]
                (let [hierarchy (reduce (fn [h {:keys [entry-id parent-id]}]
                                          (if (and entry-id parent-id)
                                            (derive h
                                                    (keyword "tag" entry-id)
                                                    (keyword "tag" parent-id))
                                            h))
                                        (make-hierarchy) (get-in db [:mymine :entries]))]
                  (update-in db [:mymine] assoc :hierarchy hierarchy))))

(reg-event-fx ::success-store-tag
              (fn [{db :db} [_ new-tags]]
                (let [new-db (update-in db [:mymine :entries] #(apply conj % new-tags))]
                  {:db new-db
                   :dispatch [::rederive]})))

(reg-event-fx ::delete-tag []
              (fn [{db :db} [_ label]]
                (let [context-menu-target (get-in db [:mymine :context-menu-target])]
                  {:db db
                   ::fx/http {:method :delete
                              :on-success [::success-delete-tag]
                              :uri (str "/api/mymine/entries/" (:entry-id context-menu-target))}})))


(defn isa-filter [root-id entry]
  (if-let [entry-id (:entry-id entry)]
    (do
      (isa? (keyword "tag" entry-id) (keyword "tag" root-id)))
    false))

(reg-event-fx ::success-delete-tag
              ; Postgres returns a collection consisting of one deleted id
              ; hence the destructuring
              (fn [{db :db} [_ [{entry-id :entry-id}]]]
                ; Remove any mymine entries that were derived from this entry
                {:db (update-in db [:mymine :entries] #(remove (partial isa-filter entry-id) %))
                 :dispatch [::rederive]}))


(reg-event-fx ::rename-tag []
              (fn [{db :db} [_ label]]
                (let [context-menu-target (get-in db [:mymine :context-menu-target])]
                  {:db db
                   ::fx/http {:method :post
                              :on-success [::success-rename-tag]
                              :uri (str "/api/mymine/entries/"
                                        (:entry-id context-menu-target)
                                        "/rename/"
                                        label)}})))

(reg-event-db ::success-rename-tag
              (fn [db [_ [{entry-id :entry-id :as response}]]]
                ; Update the appropriate entry
                (update-in db [:mymine :entries]
                           #(map (fn [e] (if (= entry-id (:entry-id e)) response e)) %))))


(reg-event-fx ::fetch-tree []
              (fn [{db :db}]
                (let [current-mine (name (get-in db [:current-mine]))]
                  {:db db
                   ::fx/http {:method :get
                              :on-success [::echo-tree]
                              :uri (str "/api/mymine/entries/" current-mine)}})))

(defn toggle-open [entries entry-id status]
  (map (fn [e] (if (= (:entry-id e) entry-id)
                 (assoc e :open status)
                 e)) entries))

(reg-event-fx ::toggle-tag-open []
              (fn [{db :db} [_ entry-id status]]
                {:db (update-in db [:mymine :entries] toggle-open entry-id status)
                 ::fx/http {:method :post
                            :uri (str "/api/mymine/entries/" entry-id "/open/" status)}}))

(reg-event-db ::set-cursor
              (fn [db [_ entry]]
                (assoc-in db [:mymine :cursor] entry)))


(defn keywordize-value
  "Recursively keywordize a value for a given key in a map
  (keywordize-filetypes {:one {:type folder :children {:three {:type file}}}
                         :two {:type file}}
  => {:one {:type :folder :children {:three {:type :file}}
      :two {:type :file}}"
  [m kw]
  (walk/postwalk #(if (and (map? %) (contains? % :file-type)) (update % kw keyword) %) m))


(reg-event-db ::echo-tree
              (fn [db [_ response]]
                (let [hierarchy (reduce (fn [h {:keys [entry-id parent-id]} response]
                                          (if (and entry-id parent-id)
                                            (derive h
                                                    (keyword "tag" entry-id)
                                                    (keyword "tag" parent-id))
                                            h))
                                        (make-hierarchy) response)]
                  (update-in db [:mymine] assoc :entries response :hierarchy hierarchy))))

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
                      hierarchy    (get-in db [:mymine :hierarchy])
                      current-mine (get-in db [:current-mine])]
                  (let [noop {:db (assoc-in db [:mymine :drag] nil)}]

                    (cond
                      (isa? hierarchy
                            (keyword "tag" (:entry-id dropping))
                            (keyword "tag" (:entry-id dragging))) noop
                      (not= "tag" (:im-obj-type dropping)) noop
                      (nil? dragging-id)
                      (assoc noop ::fx/http {:method :post
                                             :transit-params (assoc dragging :parent-id dropping-id :mine current-mine)
                                             :on-success [::success-store-tag]
                                             :uri "/api/mymine/entries"})
                      (and (not= dragging-id dropping-id))
                      (assoc noop ::fx/http {:method :post
                                             :on-success [::success-move-entry]
                                             :uri (str "/api/mymine/entries/"
                                                       dragging-id
                                                       "/move/"
                                                       dropping-id)})
                      :else noop)))))

(reg-event-fx ::success-move-entry
              (fn [{db :db} [_ [{:keys [entry-id parent-id] :as item}]]]
                (let [new-entries (map (fn [e]
                                         (if (= entry-id (:entry-id e))
                                           (assoc e :parent-id parent-id)
                                           e)) (get-in db [:mymine :entries]))
                      parent-tag  (first (filter (comp #{parent-id} :entry-id) new-entries))]
                  {:db (assoc-in db [:mymine :entries] new-entries)
                   :dispatch-n [[::rederive]
                                [::set-cursor parent-tag]]})))


(defn build-list-query [type summary-fields name title]
  {:title  title
   :from   type
   :select summary-fields
   :where  [{:path  type
             :op    "IN"
             :value name}]})


(reg-event-fx
  ::view-list-results
  (fn [{db :db} [_ {:keys [type name title source]}]]
    (let [summary-fields (get-in db [:assets :summary-fields source (keyword type)])]
      {:db       db
       :dispatch [:results/history+
                  {:source source
                   :type   :query
                   :value  (build-list-query type summary-fields name title)}]
       :navigate "/results"})))
