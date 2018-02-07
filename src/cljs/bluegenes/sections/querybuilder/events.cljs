(ns bluegenes.sections.querybuilder.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
            [cljs.core.async :as a :refer [<! close! chan]]
            [imcljs.query :as im-query]
            [imcljs.path :as im-path]
            [imcljs.fetch :as fetch]
            [imcljs.query :refer [->xml]]
            [clojure.set :refer [difference]]
            [cljs.reader :as reader]
            [bluegenes.sections.querybuilder.logic :refer [read-logic-string remove-code vec->list append-code]]
            [clojure.string :refer [join split blank?]]))

(def loc [:qb :qm])

(def not-blank? (complement blank?))

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

    (let [service (get-in db [:mines (get-in db [:current-mine]) :service])
          summary-path (im-path/adjust-path-to-last-class (:model service) (join "." view-vec))
          split-summary-path (split summary-path ".")]
      (if (not (im-path/class? (:model service) summary-path))
        {:qb/pv {:service service
                 :query {:from (first split-summary-path)
                         :select [(last split-summary-path)]}
                 :summary-path summary-path
                 :store-in view-vec}}
        {:dispatch [:qb/store-possible-values view-vec false]}))))

(reg-event-fx
  :qb/add-constraint
  (fn [{db :db} [_ view-vec]]
    (let [code (next-available-const-code (get-in db loc))]
      {:db (update-in db loc update-in (conj view-vec :constraints)
                      (comp vec conj) {:code nil :op nil :value nil})
       :dispatch [:qb/build-im-query]})))

(reg-event-fx
  :qb/remove-constraint
  (fn [{db :db} [_ path idx]]
    {:db (update-in db loc update-in (conj path :constraints) drop-nth idx)
     :dispatch [:qb/build-im-query]}))

; Do not re-count because this fires on every keystroke!
; Instead, attach (dispatch [:qb/count-query]) to the :on-blur of the constraints component
(reg-event-db
  :qb/update-constraint
  (fn [db [_ path idx constraint]]
    (let [updated-constraint (cond-> constraint
                                     (and
                                       (blank? (:code constraint))
                                       (not-blank? (:value constraint))) (assoc :code (next-available-const-code (get-in db loc)))
                                     (blank? (:value constraint)) (dissoc :code))]
      (update-in db loc assoc-in (reduce conj path [:constraints idx]) updated-constraint))))



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


(reg-event-db
  :qb/success-count
  (fn [db [_ count]]
    db))

(defn extract-constraints [[k value] total views]
  (let [new-total (conj total k)]
    (if-let [children (not-empty (select-keys value (filter (complement keyword?) (keys value))))] ; Keywords are reserved for flags
      (into [] (mapcat (fn [c] (extract-constraints c new-total (conj views (assoc value :path new-total)))) children))
      (conj views (assoc value :path new-total)))))

(reg-event-db
  :qb/success-summary
  (fn [db [_ dot-path summary]]
    (let [v (vec (butlast (split dot-path ".")))]
      (update-in db loc assoc-in (conj v :id-count) summary))))


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



(defn view-map [model q]
  (->> (map (fn [v] (split v ".")) (:select q))
       (reduce (fn [total next] (assoc-in total next {:visible true})) {})))

(defn with-constraints [model q query-map]
  (reduce (fn [total next]
            (let [path (conj (vec (split (:path next) ".")) :constraints)]
              (update-in total path (comp vec conj) (dissoc next :path)))) query-map (:where q)))

(defn treeify [model q]
  (->> (view-map model q)
       (with-constraints model q)))

(reg-event-fx
  :qb/load-query
  (fn [{db :db} [_ query]]
    (let [model (get-in db [:mines (get-in db [:current-mine]) :service :model])
          query (im-query/sterilize-query query)]
      {:db (update db :qb assoc
                   :enhance-query (treeify model query)
                   :menu (treeify model query)
                   :order (:select query)
                   :root-class (keyword (:from query))
                   :constraint-logic (read-logic-string (:constraintLogic query)))
       :dispatch [:qb/enhance-query-build-im-query true]})))

(reg-event-fx
  :qb/set-root-class
  (fn [{db :db} [_ root-class-kw]]
    (let [model (get-in db [:mines (get-in db [:current-mine]) :service :model])]
      {:db (update db :qb assoc
                   :constraint-logic nil
                   :query-is-valid? false
                   :order []
                   :preview nil
                   :enhance-query {}
                   :root-class (keyword root-class-kw)
                   :qm {root-class-kw {:visible true}})})))

(reg-event-db
  :qb/expand-path
  (fn [db [_ path]]
    (update-in db [:qb :menu] assoc-in path {:open? true})))

(reg-event-db
  :qb/expand-all
  (fn [db [_]]
    (assoc-in db [:qb :menu] (get-in db [:qb :enhance-query]))))

(reg-event-db
  :qb/enhance-query-choose-subclass
  (fn [db [_ path-vec subclass]]
    (let [enhance-query (get-in db [:qb :enhance-query])
          {current-subclass :subclass} (get-in db (concat [:qb :menu] path-vec))]
      (if (= current-subclass subclass)
        (update-in db [:qb :menu] assoc-in (conj path-vec :subclass) nil)
        (update-in db [:qb :menu] assoc-in (conj path-vec :subclass) subclass)))))

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
  "Builds path-query subclass constraints from the query structure"
  ([m] (mapcat (fn [n] (all-views n [] [])) (dissoc-keywords m)))
  ([[k properties] trail views]
   (let [next-trail (into [] (conj trail k))]
     (if (and (map? properties) (some? (not-empty (dissoc-keywords properties))))
       (mapcat #(all-views % next-trail views) (dissoc-keywords properties))
       (conj views next-trail)))))

(defn subclass-constraints
  "Builds path-query subclass constraints from the query structure"
  ([m] (mapcat (fn [n] (subclass-constraints n [] [])) m))
  ([[k {:keys [subclass] :as properties}] trail subclasses]
   (let [next-trail (into [] (conj trail k))
         next-subclasses (if subclass (conj subclasses {:path (join "." next-trail) :type subclass}) subclasses)]
     (if (map? properties)
       (mapcat #(subclass-constraints % next-trail next-subclasses) properties)
       subclasses))))

(defn regular-constraints
  "Builds path-query subclass constraints from the query structure"
  ([m] (mapcat (fn [n] (regular-constraints n [] [])) m))
  ([[k {:keys [constraints] :as properties}] trail total-constraints]
   (let [next-trail (into [] (conj trail k))
         next-constraints (reduce (fn [total next]
                                    ; Only collect constraints with a code!
                                    (if (:code next)
                                      (conj total (assoc next :path (join "." next-trail)))
                                      total))
                                  total-constraints constraints)]
     (if (not-empty (dissoc-keywords properties))
       (distinct (mapcat #(regular-constraints % next-trail next-constraints) properties))
       (distinct (concat total-constraints next-constraints))))))

(reg-event-fx
  :qb/export-query
  (fn [{db :db} [_]]
    {:db db
     :dispatch [:results/history+
                {:source (get-in db [:current-mine])
                 :type :query
                 :value (assoc
                          (get-in db [:qb :im-query])
                          :title "Custom Built Query")}]}))

(defn within? [col item]
  (some? (some #{item} col)))

(defn add-if-missing [col item]
  (if-not (within? col item)
    (conj col item)
    col))


(reg-event-fx
  :qb/enhance-query-add-view
  (fn [{db :db} [_ path-vec subclass]]
    {:db (cond-> db
                 path-vec (update-in [:qb :enhance-query] assoc-in path-vec {})
                 subclass (update-in [:qb :enhance-query] update-in (butlast path-vec) assoc :subclass subclass)
                 path-vec (update-in [:qb :order] add-if-missing (join "." path-vec))
                 )

     #_(cond-> (update-in db [:qb :enhance-query] assoc-in path-vec {})
               subclass (update-in [:qb :enhance-query] update-in (butlast path-vec) assoc :subclass subclass))
     :dispatch-n [[:qb/fetch-possible-values path-vec]
                  [:qb/enhance-query-build-im-query true]]}))



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
          model (get-in db [:mines current-mine-name :service :model])
          all-summary-fields (get-in db [:assets :summary-fields current-mine-name])
          summary-fields (get all-summary-fields (or (keyword subclass) (im-path/class model (join "." original-path-vec))))
          adjusted-views (map (partial split-and-drop-first original-path-vec) summary-fields)]
      {:db (reduce (fn [db path-vec]
                     (cond-> db
                             path-vec (update-in [:qb :enhance-query] update-in path-vec deep-merge {})
                             subclass (update-in [:qb :enhance-query] update-in (butlast path-vec) assoc :subclass subclass)
                             path-vec (update-in [:qb :order] add-if-missing (join "." path-vec))))
                   db adjusted-views)
       :dispatch [:qb/enhance-query-build-im-query true]})))

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

(reg-event-fx
  :qb/enhance-query-remove-view
  (fn [{db :db} [_ path-vec]]
    (let [trimmed (dissoc-in (get-in db [:qb :enhance-query]) path-vec)
          remaining-views (map (partial join ".") (all-views trimmed))
          new-order (->> remaining-views
                         (reduce add-if-missing (get-in db [:qb :order]))
                         (remove (partial (complement within?) remaining-views))
                         vec)
          current-codes (set (remove nil? (used-const-code (get-in db [:qb :enhance-query]))))
          remaining-codes (set (used-const-code trimmed))
          codes-to-remove (map symbol (clojure.set/difference current-codes remaining-codes))]

      {:db (update-in db [:qb] assoc
                      :enhance-query trimmed
                      :constraint-logic (reduce remove-code (get-in db [:qb :constraint-logic]) codes-to-remove)
                      :order new-order)
       :dispatch [:qb/enhance-query-build-im-query true]})))

(reg-event-fx
  :qb/enhance-query-add-constraint
  (fn [{db :db} [_ view-vec]]
    (let [code (next-available-const-code (get-in db [:qb :enhance-query]))]
      {:db (update-in db [:qb :enhance-query] update-in (conj view-vec :constraints)
                      (comp vec conj) {:code nil :op nil :value nil})
       :dispatch-n [[:cache/fetch-possible-values (join "." view-vec)]
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
    (let [add-code? (and (blank? (:code constraint)) (not-blank? (:value constraint)))
          remove-code? (and (blank? (:value constraint)) (:code constraint))]
      (let [updated-constraint
            (cond-> constraint
                    add-code? (assoc :code (next-available-const-code (get-in db [:qb :enhance-query])))
                    remove-code? (dissoc :code))]
        (cond-> db
                updated-constraint (update-in [:qb :enhance-query] assoc-in (reduce conj path [:constraints idx]) updated-constraint)
                add-code? (update-in [:qb :constraint-logic] append-code (symbol (:code updated-constraint)))
                remove-code? (update-in [:qb :constraint-logic] remove-code (symbol (:code constraint)))
                )))))

(reg-event-db
  :qb/enhance-query-clear-query
  (fn [db]
    (update-in db [:qb] assoc
               :enhance-query {}
               :order []
               :preview nil
               :constraint-logic '()
               :im-query nil
               :menu {})))


(reg-event-fx
  :qb/enhance-query-build-im-query
  (fn [{db :db} [_ fetch-preview?]]
    (let [enhance-query (get-in db [:qb :enhance-query])
          service (get-in db [:mines (get-in db [:current-mine]) :service])]

      (let [im-query (-> {:from (name (get-in db [:qb :root-class]))
                          :select (get-in db [:qb :order])
                          :constraintLogic (not-empty (str (not-empty (vec->list (get-in db [:qb :constraint-logic])))))
                          :where (concat (regular-constraints enhance-query) (subclass-constraints enhance-query))}
                         im-query/sterilize-query)
            query-changed? (not= im-query (get-in db [:qb :im-query]))]
        (cond-> {:db (update-in db [:qb] assoc :im-query im-query)}
                (and query-changed? fetch-preview?) (assoc :dispatch [:qb/fetch-preview service im-query])
                )))))


(reg-event-fx
  :qb/set-order
  (fn [{db :db} [_ ordered-vec]]
    {:db (assoc-in db [:qb :order] ordered-vec)
     :dispatch [:qb/enhance-query-build-im-query true]}))




(reg-event-db
  :qb/save-preview
  (fn [db [_ results]]
    (update db :qb assoc :preview results :fetching-preview? false)))

(reg-event-db
  :qb/make-tree
  (fn [db]
    (let [model (-> db :assets :model)] db)))

(reg-event-fx
  :qb/fetch-preview
  (fn [{db :db} [_ service query]]
    (let [new-request (fetch/table-rows service query {:size 5})]
      {:db (-> db
               (assoc-in [:qb :fetching-preview?] true)
               (update-in [:qb :preview-chan] (fnil close! (chan)))
               (assoc-in [:qb :preview-chan] new-request))
       :im-chan {:on-success [:qb/save-preview]
                 :chan new-request}})))

(reg-event-db
  :qb/enhance-query-success-summary
  (fn [db [_ dot-path summary]]
    (let [v (vec (butlast (split dot-path ".")))]
      (if summary
        (update-in db [:qb :enhance-query] assoc-in (conj v :id-count) (js/parseInt summary))
        (update-in db [:qb :enhance-query] assoc-in (conj v :id-count) nil)))))
