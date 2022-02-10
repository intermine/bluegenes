(ns bluegenes.pages.querybuilder.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
            [cljs.core.async :as a :refer [<! close! chan]]
            [imcljs.query :as im-query]
            [imcljs.path :as im-path]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [clojure.set :refer [difference]]
            [bluegenes.route :as route]
            [bluegenes.pages.querybuilder.logic :as logic
             :refer [read-logic-string remove-code vec->list append-code]]
            [clojure.string :as str :refer [join split blank? starts-with?]]
            [bluegenes.utils :refer [read-xml-query dissoc-in template->xml]]
            [oops.core :refer [oget]]
            [clojure.walk :refer [postwalk]]
            [bluegenes.components.ui.constraint :as constraint]))

(reg-event-fx
 ::load-querybuilder
 (fn [_]
   {:dispatch-n [[:qb/fetch-saved-queries]
                 [:qb/clear-import-result]]}))

(defn drop-nth
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(def alphabet (into [] "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))

(defn used-const-code
  "Walks down the query map and pulls all codes from constraints"
  [query]
  (map :code (mapcat :constraints (tree-seq map? vals query))))

(defn next-available-const-code
  "Gets the next available unused constraint letter from the query map"
  [query]
  (let [used-codes (used-const-code query)]
    (first (filter #(not (some #{%} used-codes)) alphabet))))

(reg-event-fx
 :qb/set-query
 (fn [{db :db} [_ query]]
   {:db (assoc-in db [:qb :query-map] query)
    :dispatch [:qb/build-im-query]}))

(reg-event-fx
 :qb/load-example
 (fn [{db :db}]
   (let [default-query (get-in db [:mines (get db :current-mine) :default-query-example])]
     {:dispatch [:qb/load-query default-query]})))

(reg-event-db
 :qb/store-possible-values
 (fn [db [_ view-vec results]]
   (update-in db [:qb :enhance-query] update-in view-vec assoc :possible-values (:results results))))

(reg-fx
 :qb/pv
 (fn [{:keys [service store-in summary-path query]}]
   (let [sum-chan (fetch/unique-values service query summary-path 100)]
     (go (dispatch [:qb/store-possible-values store-in (<! sum-chan)])))))

(reg-event-fx
 :qb/fetch-possible-values
 (fn [{db :db} [_ view-vec]]

   (let [service (get-in db [:mines (:current-mine db) :service])
         model (assoc (:model service) :type-constraints (get-in db [:qb :im-query :where]))
         summary-path (im-path/adjust-path-to-last-class model (join "." view-vec))
         split-summary-path (split summary-path ".")]
     (if (not (im-path/class? model summary-path))
       {:qb/pv {:service service
                :query {:from (first split-summary-path)
                        :select [(last split-summary-path)]}
                :summary-path summary-path
                :store-in view-vec}}
       {:dispatch [:qb/store-possible-values view-vec false]}))))

(reg-event-db
 :qb/update-constraint-logic
 (fn [db [_ logic]]
   (assoc-in db [:qb :constraint-logic] (str "(" logic ")"))))

(reg-event-fx
 :qb/format-constraint-logic
 (fn [{db :db} [_]]
   (let [enhance-query (get-in db [:qb :enhance-query])
         logic-vec (get-in db [:qb :constraint-logic])
         used-codes (set (used-const-code enhance-query))
         codes-in-logic-vec (set (map name (remove #{'or 'and} (flatten (read-logic-string logic-vec)))))
         codes-to-append (into (sorted-set) (difference used-codes codes-in-logic-vec))]
     {:db (assoc-in db [:qb :constraint-logic] (reduce append-code (read-logic-string logic-vec) (map symbol codes-to-append)))
      :dispatch [:qb/enhance-query-build-im-query true]})))

(defn serialize-views [[k value] total views]
  (let [new-total (vec (conj total k))]
    (if-let [children (not-empty (select-keys value (filter (complement keyword?) (keys value))))] ; Keywords are reserved for flags
      (into [] (mapcat (fn [c] (serialize-views c new-total views)) children))
      (conj views (join "." new-total)))))

(defn serialize-constraints [[k {:keys [children constraints]}] total trail]
  (if children
    (flatten (reduce (fn [t n] (conj t (serialize-constraints n total (str trail (if trail ".") k)))) total children))
    (conj total (map (fn [n] (assoc n :path (str trail (if trail ".") k))) constraints))))

(defn extract-constraints [[k value] total views]
  (let [new-total (conj total k)]
    (if-let [children (not-empty (select-keys value (filter (complement keyword?) (keys value))))] ; Keywords are reserved for flags
      (into [] (mapcat (fn [c] (extract-constraints c new-total (conj views (assoc value :path new-total)))) children))
      (conj views (assoc value :path new-total)))))

(defn remove-keyword-keys
  "Removes all keys from a map that are keywords.
  In our query map, keywords are reserved for special attributes such as :constraints and :visible"
  [m]
  (into {} (filter (comp (complement keyword?) first) m)))

(defn class-paths
  "Walks the query map and retrieves all im-paths that resolve to a class"
  ([model query]
   (let [[root children] (first query)]
     (filter (partial im-path/class? model) (map #(join "." %) (distinct (class-paths model [root children] [root] []))))))
  ([model [parent children] running total]
   (let [total (conj total running)]
     (if-let [children (not-empty (remove-keyword-keys children))]
       (mapcat (fn [[k v]] (class-paths model [k v] (conj running k) total)) children)
       total))))

(defn with-views [q query-map]
  (reduce (fn [m path]
            (assoc-in m path {:visible true}))
          query-map
          (map #(split % ".") (:select q))))

(defn with-constraints [q query-map]
  (reduce (fn [m {:keys [path] :as constraint}]
            (let [path (concat (split path ".") [:constraints])]
              (update-in m path (fnil conj []) (dissoc constraint :path))))
          query-map
          (filter (complement :type) (:where q))))

(defn with-subclasses [q query-map]
  (reduce (fn [m {:keys [path] :as constraint}]
            (let [path (concat (split path ".") [:subclass])]
              (assoc-in m path (:type constraint))))
          query-map
          (filter :type (:where q))))

(defn treeify [q]
  (->> {}
       (with-views q)
       (with-constraints q)
       (with-subclasses q)))

(reg-event-fx
 :qb/load-query
 (fn [{db :db} [_ query]]
   (let [query (im-query/sterilize-query query)
         tree (treeify query)]
     {:db (update db :qb assoc
                  :enhance-query tree
                  :menu tree
                  :order (:select query)
                  :root-class (keyword (:from query))
                  :constraint-logic (read-logic-string (:constraintLogic query))
                  :sort (:sortOrder query)
                  :joins (set (:joins query)))
      :dispatch [:qb/enhance-query-build-im-query true]})))

;; The JSON representation of template constraints is a bit different from how
;; it's in XML (why aren't they the same??) We follow the XML convention in BG.
;; Here's the mapping from XML <=> JSON template constraint representations:
;; editable=true|false <=> editable=true|false
;; no switchable <=> switchable=false switched=LOCKED
;; switchable=on <=> switchable=true switched=ON
;; switchable=off <=> switchable=true switched=OFF
(defn read-template-constraints [temp-consts]
  (mapv (fn [{:keys [editable switched description] :as const}]
          (if (:type const)
            ;; Type constraints cannot be used as template constraints.
            [const {}]
            (let [const (dissoc const :editable :switchable :switched :description)
                  switched (str/lower-case switched)]
              [const
               (-> {}
                   (assoc :editable editable
                          :description description)
                   (cond-> (#{"on" "off"} switched)
                     (assoc :switchable switched)))])))
        temp-consts))

(reg-event-fx
 :qb/load-template
 (fn [{db :db} [_ template-query]]
   (let [[consts consts-meta] ((juxt (partial mapv first)
                                     (partial mapv second))
                               (read-template-constraints (:where template-query)))
         query (assoc template-query :where consts)
         query (im-query/sterilize-query query)
         tree (treeify query)]
     {:db (update db :qb assoc
                  :enhance-query tree
                  :menu tree
                  :order (:select query)
                  :root-class (keyword (:from query))
                  :constraint-logic (read-logic-string (:constraintLogic query))
                  :sort (:sortOrder query)
                  :joins (set (:joins query))
                  :template-meta (-> template-query
                                     (select-keys [:name :title :comment :description])
                                     ;; As we treeify the query, and :im-query:where is derived from that tree, we lose the ordering of constraints. Ideally, we want template constraints to appear in the same order they did in the template view, but supporting this ordering with the way the QB has been implemented with a tree, seems to be very error prone and difficult. The proper way to achieve that would be to change the QB to support ordering of constraints, which takes much more time. For now, we won't do this, so the ordering will be different, and we'll have to map each constraint to their corresponsive meta, which is done with this map.
                                     (assoc :const->meta (zipmap consts consts-meta))))
      :dispatch [:qb/enhance-query-build-im-query true]})))

(reg-event-db
 :qb/update-template-meta
 (fn [db [_ template-details const->meta]]
   (assoc-in db [:qb :template-meta]
             (assoc template-details
                    :const->meta const->meta))))

(reg-event-db
 :qb/update-template-meta-consts
 (fn [db [_ const->meta]]
   (assoc-in db [:qb :template-meta :const->meta] const->meta)))

(reg-event-fx
 :qb/set-root-class
 (fn [{db :db} [_ root-class]]
   {:db (update db :qb assoc
                :constraint-logic nil
                :order []
                :sort []
                :joins #{}
                :preview nil
                :im-query nil
                :enhance-query {}
                :root-class (keyword root-class))}))

(reg-event-db
 :qb/expand-path
 (fn [db [_ path]]
   (update-in db [:qb :menu] assoc-in path
              (or (get-in db (concat [:qb :enhance-query] path))
                  {}))))

(reg-event-db
 :qb/expand-all
 (fn [db [_]]
   (assoc-in db [:qb :menu] (get-in db [:qb :enhance-query]))))

(defn get-subclass
  "Get subclass at path-vec in query-tree."
  [query-tree path-vec]
  (get-in query-tree (concat path-vec [:subclass])))

(defn get-all-subclasses
  "Get all subclasses that are present at any point when drilling down path-vec
  into query-tree. This translates to the subclass constraints of all parent
  classes of the path. Returns a seq of vector tuples containing the path to
  the class with the subclass constraint and the subclass constraint value."
  [query-tree path-vec]
  (let [sub-paths (->> path-vec (iterate drop-last) (take-while not-empty))]
    (keep (fn [subpath]
            (when-let [subclass (get-subclass query-tree subpath)]
              [subpath subclass]))
          sub-paths)))

(defn set-subclass
  "Set subclass at path-vec in query-tree."
  [query-tree path-vec subclass]
  (assoc-in query-tree (concat path-vec [:subclass]) subclass))

(defn clear-subclass
  "Clear subclass at path-vec in query-tree."
  [query-tree path-vec]
  (update-in query-tree path-vec dissoc :subclass))

(defn trim-subclasses
  "Remove map entries that have the value `{:subclass ,,,}` from query-tree,
  which would usually be `:enhanced-query`. This is useful as a map indicates
  that its path should be added to the query, but we don't want that when only
  a subclass is set for a class that has no attributes or child classes."
  [query-tree]
  (postwalk (fn [e]
              (when-not (and (map-entry? e)
                             (map? (val e))
                             (= (-> e val keys) [:subclass]))
                e))
            query-tree))

(reg-event-fx
 :qb/enhance-query-choose-subclass
 (fn [{db :db} [_ path-vec subclass class]]
   ;; We're setting the subclass constraint in :menu, and deleting it and its
   ;; children from :enhance-query. The event handler that adds will read the
   ;; subclass from :menu and add it to :enhance-query. The reason for this is
   ;; that a subclass constraint without views is invalid. It is also to avoid
   ;; lingering attributes from a previous subclass, that might not exist in
   ;; the new subclass.
   (let [prev-path (get-in db (concat [:qb :enhance-query] path-vec))
         path-prefix (str (join "." path-vec) ".")]
     (merge
      {:db (if (= subclass class)
             ;; Subclass constraint is being disabled.
             (-> db
                 (update-in [:qb :menu] clear-subclass path-vec)
                 (cond-> prev-path (-> (update-in [:qb :enhance-query] dissoc-in path-vec)
                                       (update-in [:qb :order] (partial filterv (complement #(starts-with? % path-prefix)))))))
             ;; Subclass constraint is either being enabled or changed to a different value.
             (-> db
                 (update-in [:qb :menu] set-subclass path-vec subclass)
                 (cond-> prev-path (-> (update-in [:qb :enhance-query] dissoc-in path-vec)
                                       (update-in [:qb :order] (partial filterv (complement #(starts-with? % path-prefix))))))))}
      ;; No point building query and fetching preview if enhance-query hasn't changed.
      (when prev-path
        {:dispatch [:qb/enhance-query-build-im-query true]})))))

(reg-event-db
 :qb/collapse-all
 (fn [db [_]]
   (assoc-in db [:qb :menu] {})))

(reg-event-db
 :qb/collapse-path
 (fn [db [_ path]]
   (update-in db [:qb :menu] update-in (butlast path) dissoc (last path))))

(defn dissoc-keywords [m]
  (when (map? m) (apply dissoc m (filter (some-fn keyword? nil?) (keys m)))))

(defn all-views
  "Builds path-query views from the query structure"
  ([m] (mapcat (fn [n] (all-views n [] [])) (dissoc-keywords m)))
  ([[k properties] trail views]
   (let [next-trail (into [] (conj trail k))]
     (if (and (map? properties) (some? (not-empty (dissoc-keywords properties))))
       (mapcat #(all-views % next-trail views) (dissoc-keywords properties))
       (conj views next-trail)))))

(defn subclass-constraints
  "Builds path-query subclass constraints from the query structure"
  ([m] (->> (mapcat (fn [n] (subclass-constraints n [] [])) m)
            ;; There's no point in keeping multiple identical constraints.
            ;; This also cleans up multiple subclass constraints, as one
            ;; is added for each attribute of a subclass (we only need one).
            (distinct)))
  ([[k {:keys [subclass] :as properties}] trail subclasses]
   (let [next-trail (into [] (conj trail k))
         next-subclasses (if subclass
                           (conj subclasses {:path (join "." next-trail) :type subclass})
                           subclasses)]
     (if (map? properties)
       (mapcat #(subclass-constraints % next-trail next-subclasses) properties)
       subclasses))))

(defn regular-constraints
  "Builds path-query regular constraints from the query structure"
  ([m] (->> (mapcat (fn [n] (regular-constraints n [] [])) m)
            ;; There's no point in keeping multiple identical constraints.
            (distinct)))
  ([[k {:keys [constraints] :as properties}] trail total-constraints]
   (let [next-trail (into [] (conj trail k))
         next-constraints (reduce (fn [total next]
                                    (if (constraint/satisfied-constraint? next)
                                      (conj total (assoc next :path (join "." next-trail)))
                                      total))
                                  total-constraints constraints)]
     (if (not-empty (dissoc-keywords properties))
       (distinct (mapcat #(regular-constraints % next-trail next-constraints) properties))
       (distinct (concat total-constraints next-constraints))))))

(reg-event-fx
 :qb/export-query
 (fn [{db :db} [_]]
   (let [query (get-in db [:qb :im-query])
         title (str "Custom " (:from query) " Query")]
     {:dispatch [:results/history+
                 {:source (get-in db [:current-mine])
                  :type :query
                  :intent :query
                  :value (assoc query
                                :title (-> (str/replace title " " "_")
                                           (str "_" (hash query))))
                  :display-title title}]})))

(defn within? [col item]
  (some? (some #{item} col)))

(defn add-if-missing [col item]
  (if-not (within? col item)
    (conj col item)
    col))

(reg-event-fx
 :qb/enhance-query-add-view
 (fn [{db :db} [_ path-vec]]
   (let [subclasses (get-all-subclasses (get-in db [:qb :menu]) path-vec)
         enhanced-db (cond-> db
                       path-vec (-> (assoc-in (into [:qb :enhance-query] path-vec) {})
                                    (update-in [:qb :order] add-if-missing (join "." path-vec)))
                       (seq subclasses) (update-in [:qb :enhance-query]
                                                   (partial reduce #(apply set-subclass %1 %2))
                                                   subclasses))]
     ;; We pass the db with updated :enhanced-query instead of updating the db
     ;; here, to avoid an invalid state where the view updating causes a model
     ;; walk to return nil due to :im-query not having a required type
     ;; constraint (which only gets added in the next event).
     {:dispatch-n [[:qb/enhance-query-build-im-query true enhanced-db]
                   ;; Note: It may be possible for the below event to fail due
                   ;; to the above. This only means possible values won't be
                   ;; found though, so not a crash.
                   [:qb/fetch-possible-values path-vec]]})))

(defn split-and-drop-first [parent-path summary-field]
  (concat parent-path ((comp vec (partial drop 1) #(clojure.string/split % ".")) summary-field)))

(defn deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(reg-event-fx
 :qb/enhance-query-add-summary-views
 (fn [{db :db} [_ original-path-vec subclass]]
   (let [current-mine-name (get db :current-mine)
         model (assoc (get-in db [:mines current-mine-name :service :model])
                      :type-constraints (logic/qb-menu->type-constraints (get-in db [:qb :menu])))
         all-summary-fields (get-in db [:assets :summary-fields current-mine-name])
         class (im-path/class model (join "." original-path-vec))
         summary-fields (get all-summary-fields (or (keyword subclass) class))
         adjusted-views (map (partial split-and-drop-first original-path-vec) summary-fields)
         subclasses (get-all-subclasses (get-in db [:qb :menu]) original-path-vec)
         enhanced-db (-> (reduce (fn [db path-vec]
                                   (if path-vec
                                     (-> db
                                         (update-in (into [:qb :enhance-query] path-vec) deep-merge {})
                                         (update-in [:qb :order] add-if-missing (join "." path-vec)))
                                     db))
                                 db
                                 adjusted-views)
                         (cond->
                           (seq subclasses) (update-in [:qb :enhance-query]
                                                       (partial reduce #(apply set-subclass %1 %2))
                                                       subclasses)))]
     ;; We pass the db with updated :enhanced-query instead of updating the db
     ;; here, to avoid an invalid state where the view updating causes a model
     ;; walk to return nil due to :im-query not having a required type
     ;; constraint (which only gets added in the next event).
     {:dispatch [:qb/enhance-query-build-im-query true enhanced-db]})))

(reg-event-fx
 :qb/enhance-query-remove-view
 (fn [{db :db} [_ path-vec]]
   (let [trimmed (trim-subclasses (dissoc-in (get-in db [:qb :enhance-query]) path-vec))
         remaining-views (map (partial join ".") (all-views trimmed))
         new-order (->> remaining-views
                        (reduce add-if-missing (get-in db [:qb :order]))
                        (remove (partial (complement within?) remaining-views))
                        vec)
         new-joins (into #{} (filter #(get-in trimmed (split % #"\."))) (get-in db [:qb :joins]))
         current-codes (set (remove nil? (used-const-code (get-in db [:qb :enhance-query]))))
         remaining-codes (set (used-const-code trimmed))
         codes-to-remove (map symbol (clojure.set/difference current-codes remaining-codes))]
     {:db (update-in db [:qb] assoc
                     :enhance-query trimmed
                     :constraint-logic (reduce remove-code (get-in db [:qb :constraint-logic]) codes-to-remove)
                     :order new-order
                     :joins new-joins)
      :dispatch [:qb/enhance-query-build-im-query true]})))

(defn subpath-of? [subpath path]
  (let [subpaths (->> (split path #"\.")
                      (iterate drop-last)
                      (take-while not-empty)
                      (set))]
    (contains? subpaths (split subpath #"\."))))

(reg-event-fx
 :qb/add-outer-join
 (fn [{db :db} [_ pathv]]
   (let [outerjoins ((fnil conj #{}) (get-in db [:qb :joins]) (join "." pathv))
         outerjoined-paths (->> (get-in db [:qb :sort])
                                (map :path)
                                (filter (fn [path]
                                          (some #(subpath-of? % path) outerjoins)))
                                (set))]
     {:db (-> db
              (assoc-in [:qb :joins] outerjoins)
              ;; If any previously added sort path is part of the newly added
              ;; outerjoined path, we need to remove it as it's not supported.
              (cond-> (not-empty outerjoined-paths)
                (update-in [:qb :sort]
                           #(into [] (remove (comp outerjoined-paths :path)) %))))
      :dispatch [:qb/enhance-query-build-im-query true]})))

(reg-event-fx
 :qb/remove-outer-join
 (fn [{db :db} [_ pathv]]
   {:db (update-in db [:qb :joins] (fnil disj #{}) (join "." pathv))
    :dispatch [:qb/enhance-query-build-im-query true]}))

(reg-event-fx
 :qb/enhance-query-add-constraint
 (fn [{db :db} [_ view-vec]]
   (let [model (get-in db [:mines (:current-mine db) :service :model])
         constraints (get-in db [:qb :im-query :where])]
     {:db (update-in db [:qb :enhance-query] update-in (conj view-vec :constraints)
                     (comp vec conj) {:code nil :op nil :value nil})
      :dispatch-n [[:cache/fetch-possible-values (join "." view-vec) model constraints]
                   [:qb/fetch-possible-values view-vec]]})))
;:dispatch [:qb/build-im-query]


(reg-event-fx
 :qb/enhance-query-remove-constraint
 (fn [{db :db} [_ path idx]]

   (let [dropped-code (get-in db (concat [:qb :enhance-query] (conj path :constraints) [idx :code]))]
     {:db (-> db
              (update-in [:qb :enhance-query] update-in (conj path :constraints) drop-nth idx)
              (update-in [:qb :constraint-logic] remove-code (when dropped-code (symbol dropped-code))))
      :dispatch [:qb/enhance-query-build-im-query true]})))
;:dispatch [:qb/build-im-query]

(reg-event-db
 :qb/enhance-query-update-constraint
 (fn [db [_ path idx constraint]]
   (let [add-code? (and (blank? (:code constraint))
                        (constraint/satisfied-constraint? constraint))
         remove-code? (and (not (blank? (:code constraint)))
                           (not (constraint/satisfied-constraint? constraint)))
         constraint-path (concat [:qb :enhance-query] path [:constraints idx])
         old-constraint (get-in db constraint-path)
         updated-constraint (cond-> (constraint/clear-constraint-value old-constraint constraint)
                              add-code? (assoc :code (next-available-const-code (get-in db [:qb :enhance-query])))
                              remove-code? (dissoc :code))]
     (cond-> db
       updated-constraint (assoc-in constraint-path updated-constraint)
       add-code? (update-in [:qb :constraint-logic] append-code (symbol (:code updated-constraint)))
       remove-code? (update-in [:qb :constraint-logic] remove-code (symbol (:code constraint)))))))

(reg-event-db
 :qb/enhance-query-clear-query
 (fn [db]
   (update-in db [:qb] assoc
              :enhance-query {}
              :order []
              :sort []
              :joins #{}
              :preview nil
              :constraint-logic '()
              :im-query nil
              :menu {})))

(defn enhance-constraint-logic
  "If you have read the surrounding code, you'll know that the Query Builder
  requires an extended version of PathQuery object to accomodate the interface.
  This is the 'enhanced' query, which is different from how you usually see
  PathQuery encoded in JSON. Likewise, constraint logics have an 'enhanced'
  representation which this function returns."
  [logic]
  (not-empty (str (not-empty (vec->list logic)))))

(reg-event-fx
 :qb/enhance-query-build-im-query
 (fn [{db :db} [_ fetch-preview? ?enhanced-db]]
   ;; Passing ?enhanced-db allows the event handler dispatching this event
   ;; handler to avoid updating the view, until :im-query gets updated.
   (let [db (or ?enhanced-db db)
         enhance-query (get-in db [:qb :enhance-query])
         service (get-in db [:mines (get-in db [:current-mine]) :service])
         im-query (-> {:from (name (get-in db [:qb :root-class]))
                       :select (get-in db [:qb :order])
                       :constraintLogic (enhance-constraint-logic (get-in db [:qb :constraint-logic]))
                       :where (concat (regular-constraints enhance-query) (subclass-constraints enhance-query))
                       :sortOrder (get-in db [:qb :sort])
                       :joins (vec (get-in db [:qb :joins]))}
                      (im-query/sterilize-query))
         query-changed? (not= im-query (get-in db [:qb :im-query]))]
     (cond-> {:db (update-in db [:qb] assoc :im-query im-query)}
       fetch-preview? (assoc :dispatch [:qb/fetch-preview service im-query])))))

(reg-event-fx
 :qb/set-order
 (fn [{db :db} [_ ordered-vec]]
   {:db (assoc-in db [:qb :order] ordered-vec)
    :dispatch [:qb/enhance-query-build-im-query true]}))

(reg-event-fx
 :qb/set-sort
 (fn [{db :db} [_ path direction]]
   {:db (update-in db [:qb :sort]
                   (fn [sorts]
                     (let [match (first (filter (comp #{path} :path) sorts))]
                       (cond
                         ;; Remove active sorting.
                         (and match (= direction (:direction match)))
                         (into [] (remove (comp #{path} :path)) sorts)
                         ;; Change active sorting.
                         match
                         (mapv #(cond-> %
                                  (= path (:path %))
                                  (assoc :direction direction))
                               sorts)
                         ;; Add new sorting.
                         :else (conj sorts {:path path :direction direction})))))
    :dispatch [:qb/enhance-query-build-im-query true]}))

(reg-event-db
 :qb/save-preview
 (fn [db [_ results]]
   (update db :qb assoc
           :preview results
           :preview-error nil
           :fetching-preview? false)))

(reg-event-db
 :qb/failure-preview
 (fn [db [_ res]]
   (update db :qb assoc
           :preview nil
           :preview-error (or (get-in res [:body :error]) (pr-str res))
           :fetching-preview? false)))

(reg-event-fx
 :qb/fetch-preview
 (fn [{db :db} [_ service query]]
   (let [new-request (fetch/table-rows service query {:size 5})]
     {:db (-> db
              (assoc-in [:qb :fetching-preview?] true)
              (update-in [:qb :preview-chan] (fnil close! (chan)))
              (assoc-in [:qb :preview-chan] new-request))
      :im-chan {:on-success [:qb/save-preview]
                :on-failure [:qb/failure-preview]
                :chan new-request}})))

(reg-event-db
 :qb/enhance-query-success-summary
 (fn [db [_ dot-path summary]]
   (let [v (vec (butlast (split dot-path ".")))]
     (if summary
       (update-in db [:qb :enhance-query] assoc-in (conj v :id-count) (js/parseInt summary))
       (update-in db [:qb :enhance-query] assoc-in (conj v :id-count) nil)))))

(reg-event-fx
 :qb/save-query
 (fn [{db :db} [_ title]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         query (get-in db [:qb :im-query])]
     {:im-chan {:chan (save/query service (assoc query :title title))
                :on-success [:qb/save-query-success]
                :on-failure [:qb/save-query-failure title]}
      :dispatch [:messages/add
                 {:markup [:span "Your query titled " [:em title]
                           " has been saved. You can access it under the "
                           [:strong "Saved Queries"] " tab."]}
                 :style "success"]})))

(reg-event-fx
 :qb/save-query-success
 (fn [{db :db} [_]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (fetch/saved-queries service)
                :on-success [:qb/fetch-saved-queries-success]
                :on-failure [:qb/fetch-saved-queries-failure]}})))

(reg-event-fx
 :qb/save-query-failure
 (fn [_ [_ title res]]
   {:dispatch [:messages/add
               {:markup [:span (str "Failed to save query '" title "'. "
                                    (or (get-in res [:body :error])
                                        "Please check your connection and try again."))]
                :style "warning"}]}))

(reg-event-fx
 :qb/delete-query
 (fn [{db :db} [_ title]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (save/delete-query service title)
                :on-success [:qb/delete-query-success title]
                :on-failure [:qb/delete-query-failure title]}})))

(reg-event-fx
 :qb/delete-query-success
 (fn [{db :db} [_ title _res]]
   (let [query (get-in db [:qb :saved-queries title])]
     {:db (update-in db [:qb :saved-queries] dissoc title)
      :dispatch [:messages/add
                 {:markup (fn [id]
                            [:span
                             "The query "
                             [:em title]
                             " has been deleted from your user profile. "
                             [:a {:role "button"
                                  :on-click #(dispatch [:qb/undo-delete-query
                                                        title query id])}
                              "Click here"]
                             " to undo this action and restore your query."])
                  :style "info"
                  :timeout 10000}]})))

(reg-event-fx
 :qb/undo-delete-query
 (fn [{db :db} [_ title query id]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (save/query service (assoc query :title title))
                :on-success [:qb/save-query-success]
                :on-failure [:qb/save-query-failure title]}
      :dispatch [:messages/remove id]})))

(reg-event-fx
 :qb/delete-query-failure
 (fn [_ [_ title res]]
   {:dispatch [:messages/add
               {:markup [:span (str "Failed to delete query '" title "'. "
                                    (or (get-in res [:body :error])
                                        "Please check your connection and try again."))]
                :style "warning"}]}))

(reg-event-fx
 :qb/rename-query
 (fn [{db :db} [_ old-title new-title]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         query (get-in db [:qb :saved-queries old-title])]
     {:db (update-in db [:qb :saved-queries] dissoc old-title)
      :im-chan {:chan (save/query service (assoc query :title new-title))
                :on-success [:qb/rename-query-success old-title query]
                :on-failure [:qb/rename-query-failure old-title query]}})))

(reg-event-fx
 :qb/rename-query-success
 (fn [{db :db} [_ old-title query]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (save/delete-query service old-title)
                :on-success [:qb/save-query-success]
                :on-failure [:qb/rename-query-failure old-title query]}})))

(reg-event-fx
 :qb/rename-query-failure
 (fn [{db :db} [_ old-title query]]
   {:db (assoc-in db [:qb :saved-queries old-title] query)
    :dispatch [:messages/add
               {:markup [:span (str "An error occured when renaming saved query '" old-title "'.")]
                :style "warning"}]}))

(reg-event-fx
 :qb/fetch-saved-queries
 (fn [{db :db} [_]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (update db :qb dissoc :saved-queries)
      :im-chan {:chan (fetch/saved-queries service)
                :on-success [:qb/fetch-saved-queries-success]
                :on-failure [:qb/fetch-saved-queries-failure]}})))

(reg-event-db
 :qb/fetch-saved-queries-success
 (fn [db [_ queries]]
   (assoc-in db [:qb :saved-queries]
             (reduce-kv
              (fn [m title query]
                (assoc m (name title)
                       (let [path (-> query :select first)
                             root (first (split path #"\."))]
                         (-> query
                             (assoc :from root)
                             (update :constraintLogic
                                     (comp enhance-constraint-logic read-logic-string))
                             (update :where #(or % []))
                             (dissoc :title :model)
                             ;; Sterilizing *might* not be necessary since
                             ;; it's coming straight from the InterMine.
                             (im-query/sterilize-query)))))
              {} queries))))

(reg-event-fx
 :qb/import-xml-query
 (fn [{db :db} [_ query-xml]]
   (try
     (let [query (read-xml-query query-xml)]
       {:db (assoc-in db [:qb :import-result] {:type "success"
                                               :text "XML loaded successfully"})
        :dispatch [:qb/load-query query]})
     (catch js/Error e
       {:db (assoc-in db [:qb :import-result] {:type "failure"
                                               :text (oget e :message)})}))))

(reg-event-db
 :qb/clear-import-result
 (fn [db [_]]
   (update db :qb dissoc :import-result)))

(reg-event-db
 :qb/swap-constraints-ordering
 (fn [db [_ i1 i2]]
   (update-in db [:qb :im-query :where] logic/vec-swap-indices i1 i2)))

(reg-event-fx
 :qb/save-template
 (fn [{db :db} [_ template-details template-constraints]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         model (:model service)
         query (assoc (get-in db [:qb :im-query])
                      :where template-constraints)
         template-query (template->xml model template-details query)]
     {:im-chan {:chan (save/template service template-query)
                :on-success [:qb/save-template-success (:name template-details)]
                :on-failure [:qb/save-template-failure (:name template-details)]}})))

(reg-event-fx
 :qb/save-template-success
 (fn [{db :db} [_ template-name _res]]
   {:dispatch-n [[:assets/fetch-templates]
                 [:messages/add
                  {:markup [:span "Saved template: "
                            [:a {:href (route/href ::route/template {:template template-name})}
                             template-name]]
                   :style "success"}]]}))

;; Also dispatch from :templates/undo-delete-template
(reg-event-fx
 :qb/save-template-failure
 (fn [{db :db} [_ template-name res]]
   {:dispatch [:messages/add
               {:markup [:span [:strong "Failed to save template " [:em template-name]] " "
                         (when-let [err (get-in res [:body :error])]
                           [:code err])]
                :timeout 10000
                :style "danger"}]}))
