(ns redgenes.sections.querybuilder.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.query :as im-query]
            [imcljs.fetch :as fetch]
            [clojure.string :refer [join split]]))

(reg-event-db
  :qb/set-query
  (fn [db [_ query]]
    (assoc-in db [:qb :query-map] query)))

(reg-event-db
  :qb/add-view
  (fn [db [_ view-vec]]
    (let [true-path (-> (interpose :children view-vec) vec)]
      (update-in db (concat [:qb :qm] true-path) assoc :visible true))))

(reg-event-db
  :qb/remove-view
  (fn [db [_ view-vec]]
    (let [true-path (-> (interpose :children view-vec) vec)]
      ; Recursively drop parent nodes if they're empty
      (loop [db db path true-path]
        (let [new (update-in db (concat [:qb :qm] (butlast path)) dissoc (last path))]
          (if (and (> (count (butlast path)) 1) ; Don't drop the root node infiniloop!
                   (empty? (get-in new (concat [:qb :qm] (butlast path)))))
            (recur new (butlast path))
            new))))))

(reg-event-db
  :qb/toggle-view
  (fn [db [_ view-vec]]
    (let [true-path (-> (interpose :children view-vec) vec (conj :visible))]
      (update-in db (concat [:qb :qm] true-path) not))))

(reg-event-db
  :qb/add-constraint
  (fn [db [_ view-vec]]
    (let [true-path (-> (interpose :children view-vec) vec (conj :constraints))]
      (update-in db (concat [:qb :qm] true-path) (comp vec conj) {:op "=" :value nil}))))


(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))


(reg-event-db
  :qb/remove-constraint
  (fn [db [_ path idx]]
    (println "REMOVING")
    (let [true-path (-> (interpose :children path) vec (conj :constraints))]
      (.log js/console "fin" (update-in db (concat [:qb :qm] true-path) vec-remove idx))
      (update-in db (concat [:qb :qm] true-path) vec-remove idx))))

(reg-event-db
  :qb/update-constraint
  (fn [db [_ path idx constraint]]
    (let [true-path (-> (interpose :children path) vec (conj :constraints) (conj idx))]
      (assoc-in db (concat [:qb :qm] true-path) constraint))))

(reg-event-db
  :qb/update-constraint
  (fn [db [_ path idx constraint]]
    (let [true-path (-> (interpose :children path) vec (conj :constraints) (conj idx))]
      (assoc-in db (concat [:qb :qm] true-path) constraint))))

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

(defn make-query [query-map query-constraints]
  {:from   "Gene"
   :select (map-view->dot query-map)
   :where  query-constraints})


(defn map-depth
  "Returns the depth of a map."
  ([m]
   (map-depth m 1))
  ([m current-depth]
   (apply max (flatten (reduce (fn [total [k v]]
                                 (if (map? v)
                                   (conj total (map-depth v (inc current-depth)))
                                   (conj total current-depth))) [] m)))))

(defn serialize-views [[k {:keys [children visible]}] total trail]
  (if visible
    (str trail "." k)
    (flatten (reduce (fn [t n] (conj t (serialize-views n total (str trail (if trail ".") k)))) total children))))

(defn serialize-constraints [[k {:keys [children constraints]}] total trail]
  (if children
    (flatten (reduce (fn [t n] (conj t (serialize-constraints n total (str trail (if trail ".") k)))) total children))
    (conj total (map (fn [n]
                       (.log js/console "n" n)
                       (assoc n :path (str trail (if trail ".") k))) constraints))))


(reg-event-db
  :qb/success-count
  (fn [db [_ count]]
    (println "count" count)
    db))



(def test-query {:path        "Gene"
                 :visible     true
                 :constraints [{:op    "IN"
                                :value "My Favorite List"}]
                 :children    [{:path        "symbol"
                                :visible     true
                                :constraints [{:op    "="
                                               :value "zen"}]}
                               {:path     "organism"
                                :visible  true
                                :children [{:path        "name"
                                            :visible     true
                                            :constraints [{:op    "="
                                                           :value "Homo sapiens"}]}]}]})

(defn extract-constraints
  ([query]
   (distinct (extract-constraints nil [] query)))
  ([s c {:keys [path children constraints]}]
   (let [dot (str s (if s ".") path)] ; Our path so far (Gene.alleles)
     (if children
       (mapcat (partial extract-constraints dot (reduce conj c (map #(assoc % :path dot) constraints))) children)
       (reduce conj c (map #(assoc % :path dot) constraints))))))

(reg-event-fx
  :qb/make-query
  (fn [{db :db}]
    (let [service (get-in db [:mines (get-in db [:current-mine]) :service])
          query   {:select (reduce conj (map serialize-views (get-in db [:qb :qm])))
                   :where  (reduce conj (map serialize-constraints (get-in db [:qb :qm])))}]

      (.log js/console "looped" (extract-constraints test-query))

      {:db db
       ;:im-operation {:on-success [:qb/success-count]
       ;               :op         (partial fetch/row-count
       ;                                    service
       ;                                    query)}
       })))

