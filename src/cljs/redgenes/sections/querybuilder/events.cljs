(ns redgenes.sections.querybuilder.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
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




(reg-event-db
  :qb/make-query
  (fn [db]
    db))

