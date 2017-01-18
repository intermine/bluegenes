(ns redgenes.sections.querybuilder.events
  (:require [re-frame.core :refer [reg-event-db]]
            [clojure.string :refer [join split]]))

(reg-event-db
  :qb/set-query
  (fn [db [_ query]]
    (assoc-in db [:qb :query-map] query)))

(reg-event-db
  :qb/add-view
  (fn [db [_ view-vec]]
    (update-in db [:qb :query-map] assoc-in view-vec true)))

(reg-event-db
  :qb/remove-view
  (fn [db [_ view-vec]]
    (-> db
        ; First remove the view
        (update-in [:qb :query-map] update-in (drop-last view-vec) dissoc (last view-vec))
        ; Then remove any constraints
        (update-in [:qb :query-constraints] (partial remove #(= (:path %) (join "." view-vec))))
        )))

(reg-event-db
  :qb/add-constraint
  (fn [db [_ view-vec]]
    (update-in db [:qb :query-constraints] conj {:path  (join "." view-vec)
                                                 :op    "="
                                                 :value nil})))


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
    (let [{:keys [query-map query-constraints]} (:qb db)]
      ;(.log js/console "qm" (make-query query-map query-constraints))
      (.log js/console "cd" (map-depth {1 true
                                        2 {3 {4 {5 {6 {7 true}}}}}}))
      )
    db))

