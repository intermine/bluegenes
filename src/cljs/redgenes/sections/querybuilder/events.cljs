(ns redgenes.sections.querybuilder.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx dispatch]]
            [cljs.core.async :refer [<!]]
            [imcljs.query :as im-query]
            [imcljs.path :as im-path]
            [imcljs.fetch :as fetch]
            [cljs.reader :as reader]
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
    {:db       (assoc-in db [:qb :query-map] query)
     :dispatch [:qb/build-im-query]}))

(reg-event-fx
  :qb/add-view
  (fn [{db :db} [_ view-vec]]
    {:db         (update-in db loc assoc-in (conj view-vec :visible) true)
     :dispatch-n [[:qb/fetch-possible-values view-vec]
                  [:qb/build-im-query]]}))

(reg-event-db
  :qb/store-possible-values
  (fn [db [_ view-vec results]]
    (update-in db [:qb :qm] update-in view-vec assoc :possible-values (:results results))))

(reg-fx
  :qb/pv
  (fn [{:keys [service view-vec]}]
    (let [str-path (join "." view-vec)]
      (println "str-path" str-path)
      (when-not (im-path/class? (:model service) str-path)
        (let [count-chan (fetch/row-count service {:from (first view-vec) :select [str-path]})]
          (go (when (> 100 (<! count-chan))
                (dispatch [:qb/store-possible-values view-vec (<! (fetch/possible-values service str-path))]))))))))


(reg-event-fx
  :qb/fetch-possible-values
  (fn [{db :db} [_ view-vec]]
    (let [service (get-in db [:mines (get-in db [:current-mine]) :service])]
      {:qb/pv {:service  service
               :view-vec view-vec}})))

;{:im-operation {:on-success [:qb/store-possible-values view-vec]
;                :op         (partial fetch/possible-values service (join "." view-vec))}}



(reg-event-fx
  :qb/remove-view
  (fn [{db :db} [_ view-vec]]
    {:db       (loop [db db path view-vec]
                 (let [new (update-in db (concat loc (butlast path)) dissoc (last path))]
                   (if (and (> (count (butlast path)) 1)    ; Don't drop the root node infiniloop!
                            (empty? (get-in new (concat [:qb :qm] (butlast path)))))
                     (recur new (butlast path))
                     new)))
     :dispatch [:qb/build-im-query]}))

(reg-event-fx
  :qb/toggle-view
  (fn [{db :db} [_ view-vec]]
    {:db       (update-in db loc update-in (conj view-vec :visible) not)
     :dispatch [:qb/build-im-query]}))

(reg-event-fx
  :qb/add-constraint
  (fn [{db :db} [_ view-vec]]
    (let [code (next-available-const-code (get-in db loc))]
      {:db       (update-in db loc update-in (conj view-vec :constraints) (comp vec conj) {:code nil :op "=" :value nil})
       :dispatch [:qb/build-im-query]})))

(reg-event-fx
  :qb/remove-constraint
  (fn [{db :db} [_ path idx]]
    {:db       (update-in db loc update-in (conj path :constraints) drop-nth idx)
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
    (assoc-in db [:qb :constraint-logic] logic)))


(defn map-view->dot
  "Turn a map of nested views into dot notation.
  (map-view->dot {Gene {id true organism {name true}}}
  => (Gene.id Gene.organism.name)"
  ([query-map]
   (map-view->dot query-map nil))
  ([query-map string-path]
   (flatten (reduce (fn [total [k v]]
                      (if (map? v)
                        (conj total (map-view->dot v (str string-path (if-not (nil? string-path) ".") k)))
                        (conj total (str string-path (if-not (nil? string-path) ".") k)))) [] query-map))))



(defn map-depth
  "Returns the depth of a map."
  ([m]
   (map-depth m 1))
  ([m current-depth]
   (apply max (flatten (reduce (fn [total [k v]]
                                 (if (map? v)
                                   (conj total (map-depth v (inc current-depth)))
                                   (conj total current-depth))) [] m)))))

;(defn serialize-views [[k {:keys [children visible]}] total trail]
;  (if visible
;    (str trail "." k)
;    (flatten (reduce (fn [t n] (conj t (serialize-views n total (str trail (if trail ".") k)))) total children))))

#_(defn serialize-views [[k value] total trail]
    (if-let [children (not-empty (select-keys value (filter (complement keyword?) (keys value))))]
      (println "children" children))
    #_(if visible
        (str trail "." k)
        (flatten (reduce (fn [t n] (conj t (serialize-views n total (str trail (if trail ".") k)))) total children))))

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







;(defn extract-constraints-old
;  ([query]
;   (distinct (extract-constraints-old nil [] query)))
;  ([s c {:keys [path children constraints]}]
;   (let [dot (str s (if s ".") path)] ; Our path so far (Gene.alleles)
;     (if children
;       (mapcat (partial extract-constraints-old dot (reduce conj c (map #(assoc % :path dot) constraints))) children)
;       (reduce conj c (map #(assoc % :path dot) constraints))))))

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


(defn make-query [model root-class query constraintLogic]
  (let [constraints (remove (fn [c] (= nil (:value c))) (remove empty? (mapcat (fn [n]
                                                                                 (map (fn [c]
                                                                                        (assoc c :path (join "." (:path n)))) (:constraints n)))
                                                                               (extract-constraints (first query) [] []))))]
    ;(println "CONSTRAINTS" constraints)
    (cond-> {:select (serialize-views (first query) [] [])}
            (not-empty constraints) (assoc :where constraints)
            constraintLogic (assoc :constraintLogic constraintLogic))))

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

(defn countable-views
  "Retrieve all im-paths that can be counted in the query map"
  [model query]
  ;(println "classpaths" (class-paths model query))
  (map #(str % ".id") (class-paths model query)))

(defn get-letters [query]
  (let [logic (reader/read-string "(A and B or C or D)")]))


(reg-event-fx
  :qb/summarize-view
  (fn [{db :db} [_ view]]
    (let [
          service (get-in db [:mines (get-in db [:current-mine]) :service])
          id-path (str (im-path/trim-to-last-class (:model service) (join "." view)) ".id")
          query   (make-query (:model service) (get-in db [:qm :root-class]) (get-in db loc) (get-in db [:qb :constraint-logic]))]
      {:db           db
       :im-operation {:on-success [:qb/success-summary id-path]
                      :op         (partial fetch/row-count
                                           service
                                           (assoc query :select [id-path]))}})))





(def aquery {:from            "Gene"
             :constraintLogic "A or B"
             :select          ["symbol"
                               "organism.name"
                               "alleles.name"
                               "alleles.dataSets.description"]
             :where           [{:path  "Gene.symbol"
                                :op    "="
                                :code  "A"
                                :value "zen"}
                               {:path  "Gene.symbol"
                                :op    "="
                                :code  "B"
                                :value "mad"}]})

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
      {:db       (update db :qb assoc
                         :mappy (treeify model query)
                         :root-class (keyword (:from query))
                         :constraint-logic (:constraintLogic query))
       :dispatch [:qb/mappy-build-im-query]})))

(reg-event-fx
  :qb/set-root-class
  (fn [{db :db} [_ root-class-kw]]
    (let [model (get-in db [:mines (get-in db [:current-mine]) :service :model])]
      {:db       (update db :qb assoc
                         :constraint-logic nil
                         :query-is-valid? false
                         :mappy {}
                         :root-class (keyword root-class-kw)
                         :qm {root-class-kw {:visible true}})
       :dispatch [:qb/count-query]})))




(defn has-views? [model query]
  "True if a query has at least one view. Queries without views are invalid."
  (not (empty? (filter (partial (complement im-path/class?) model) (:select query)))))

(reg-event-fx
  :qb/build-im-query
  (fn [{db :db}]
    (let [service (get-in db [:mines (get-in db [:current-mine]) :service])
          query   (make-query (:model service) (get-in db [:qm :root-class]) (get-in db loc) (get-in db [:qb :constraint-logic]))]
      {:db       (update db :qb assoc
                         :im-query (im-query/sterilize-query query)
                         :query-is-valid? (has-views? (:model service) query))
       :dispatch [:qb/count-query]})))


(reg-event-fx
  :qb/export-query
  (fn [{db :db} [_]]
    (when (get-in db [:qb :query-is-valid?])
      {:dispatch [:results/set-query
                  {:source :flymine-beta
                   :type   :query
                   :value  (get-in db [:qb :im-query])}]
       :navigate (str "results")})))


(reg-event-fx
  :qb/mappy-add-view
  (fn [{db :db} [_ path-vec]]
    (println "PATH" path-vec)
    {:db       (update-in db [:qb :mappy] assoc-in path-vec {})
     :dispatch [:qb/mappy-build-im-query]}))



(reg-event-fx
  :qb/mappy-remove-view
  (fn [{db :db} [_ path-vec]]
    {:db (update-in db [:qb :mappy] update-in (butlast path-vec) dissoc (last path-vec))
     :dispatch [:qb/mappy-build-im-query]}))

(reg-event-fx
  :qb/mappy-add-constraint
  (fn [{db :db} [_ view-vec]]
    (let [code (next-available-const-code (get-in db loc))]
      (println "view-bec" view-vec)
      {:db (update-in db [:qb :mappy] update-in (conj view-vec :constraints) (comp vec conj) {:code nil :op "=" :value nil})})))
;:dispatch [:qb/build-im-query]


(reg-event-fx
  :qb/mappy-remove-constraint
  (fn [{db :db} [_ path idx]]
    {:db (update-in db [:qb :mappy] update-in (conj path :constraints) drop-nth idx)}))
;:dispatch [:qb/build-im-query]





(reg-event-db
  :qb/mappy-update-constraint
  (fn [db [_ path idx constraint]]
    (let [updated-constraint (cond-> constraint
                                     (and
                                       (blank? (:code constraint))
                                       (not-blank? (:value constraint))) (assoc :code (next-available-const-code (get-in db [:qb :mappy])))
                                     (blank? (:value constraint)) (dissoc :code))]
      (update-in db [:qb :mappy] assoc-in (reduce conj path [:constraints idx]) updated-constraint))))


(defn within? [col item]
  (some? (some #{item} col)))

(defn add-if-missing [col item]
  (if-not (within? col item)
    (conj col item)
    col))


(reg-event-fx
  :qb/mappy-build-im-query
  (fn [{db :db}]
    (let [service (get-in db [:mines (get-in db [:current-mine]) :service])
          query   (make-query
                    (:model service)
                    (get-in db [:qm :root-class])
                    (get-in db [:qb :mappy]) (get-in db [:qb :constraint-logic]))]
      (let [new-order (reduce add-if-missing (get-in db [:qb :order]) (:select query))]
        (println "new-order" new-order)
        (println "removed" (remove (partial (complement within?) (:select query)) new-order)))

      {:db       (update db :qb assoc
                         :im-query (im-query/sterilize-query query)
                         :order (->> (:select query)
                                     (reduce add-if-missing (get-in db [:qb :order]))
                                     (remove (partial (complement within?) (:select query)))
                                     vec)
                         :query-is-valid? (has-views? (:model service) query))
       :dispatch [:qb/mappy-count-query service query]})))


(reg-event-db
  :qb/set-order
  (fn [db [_ ordered-vec]]
    (assoc-in db [:qb :order] ordered-vec)))


(defn countable-paths
  "Given a collection of string paths, return a collection of paths representing
  the classes appended with .id"
  [model views]
  (->> views
       (map (partial im-path/trim-to-last-class model))
       distinct
       (map #(str % ".id"))))


(reg-event-db
  :qb/save-preview
  (fn [db [_ results]]
    (assoc-in db [:qb :preview] results)))

(reg-event-fx
  :qb/fetch-preview
  (fn [{db :db} [_ service query]]
    (.log js/console "previewing" query)
    {:im-operation {:on-success [:qb/save-preview]
                    :op         (partial fetch/table-rows service query {:size 5})}}))

(reg-event-fx
  :qb/mappy-count-query
  (fn [{db :db} [_ service query]]

    (let [id-paths (countable-paths (:model service) (:select query))]
      (.log js/console "id-paths" id-paths)
      {:db             db
       :dispatch       [:qb/fetch-preview service query]
       :im-operation-n (map (fn [id-path]
                              {:on-success [:qb/mappy-success-summary id-path]
                               :op         (partial fetch/row-count
                                                    service
                                                    (assoc query :select [id-path]))}) id-paths)})))

(reg-event-db
  :qb/mappy-success-summary
  (fn [db [_ dot-path summary]]
    (let [v (vec (butlast (split dot-path ".")))]
      (update-in db [:qb :mappy] assoc-in (conj v :id-count) summary))))