(ns redgenes.sections.querybuilder.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.query :as im-query]
            [imcljs.path :as im-path]
            [imcljs.fetch :as fetch]
            [clojure.string :refer [join split]]))

(def loc [:qb :qm])

(defn drop-nth
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(reg-event-db
  :qb/set-query
  (fn [db [_ query]]
    (assoc-in db [:qb :query-map] query)))

(reg-event-db
  :qb/add-view
  (fn [db [_ view-vec]]
    (update-in db loc assoc-in (conj view-vec :visible) true)))

(reg-event-fx
  :qb/remove-view
  (fn [{db :db} [_ view-vec]]
    {:db (loop [db db path view-vec]
       (let [new (update-in db (concat loc (butlast path)) dissoc (last path))]
         (if (and (> (count (butlast path)) 1) ; Don't drop the root node infiniloop!
                  (empty? (get-in new (concat [:qb :qm] (butlast path)))))
           (recur new (butlast path))
           new)))
     :dispatch [:qb/count-query]}))

(reg-event-db
  :qb/toggle-view
  (fn [db [_ view-vec]]
    (update-in db loc update-in (conj view-vec :visible) not)))

(reg-event-db
  :qb/add-constraint
  (fn [db [_ view-vec]]
    (update-in db loc update-in (conj view-vec :constraints) (comp vec conj) {:op "=" :value nil})))

(reg-event-db
  :qb/remove-constraint
  (fn [db [_ path idx]]
    (update-in db loc update-in (conj path :constraints) drop-nth idx)))

(reg-event-db
  :qb/update-constraint
  (fn [db [_ path idx constraint]]
    (update-in db loc assoc-in (reduce conj path [:constraints idx]) constraint)))


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
    (conj total (map (fn [n]
                       (.log js/console "n" n)
                       (assoc n :path (str trail (if trail ".") k))) constraints))))


(reg-event-db
  :qb/success-count
  (fn [db [_ count]]
    (println "count" count)
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


(defn make-query [query]
  {:from   "Gene"
   :select (serialize-views (first query) [] [])
   :where  (remove empty? (mapcat (fn [n] (map (fn [c]
                                                 (assoc c :path (join "." (:path n)))) (:constraints n)))
                                  (extract-constraints (first query) [] [])))})

(defn countable-views [model query]
  (let [views (serialize-views (first query) [] [])]
    (map (comp #(str % ".id") (partial im-path/trim-to-last-class model)) views)))


(reg-event-fx
  :qb/summarize-view
  (fn [{db :db} [_ view]]
    (let [query   (make-query (get-in db loc))
          service (get-in db [:mines (get-in db [:current-mine]) :service])
          id-path (str (im-path/trim-to-last-class (:model service) (join "." view)) ".id")]
      {:db           db
       :im-operation {:on-success [:qb/success-summary id-path]
                      :op         (partial fetch/row-count
                                           service
                                           (assoc query :select [id-path]))}})))

;(partial fetch/row-count
;         service
;         (assoc query :select [(join "." view)])
;         #_{:summaryPath (join "." view)
;            :format      "jsonrows"})

(reg-event-fx
  :qb/count-query
  (fn [{db :db}]
    (let [service  (get-in db [:mines (get-in db [:current-mine]) :service])
          query    (make-query (get-in db loc))
          id-paths (countable-views (:model service) (get-in db loc))]

      {:db             db
       :im-operation-n (map (fn [id-path]
                              {:on-success [:qb/success-summary id-path]
                               :op         (partial fetch/row-count
                                                    service
                                                    (assoc query :select [id-path]))}) id-paths)})))

(reg-event-fx
  :qb/make-query
  (fn [{db :db}]
    (let [service  (get-in db [:mines (get-in db [:current-mine]) :service])
          query    (make-query (get-in db loc))
          id-paths (countable-views (:model service) (get-in db loc))]

      {:db             db
       :im-operation-n (map (fn [id-path]
                              {:on-success [:qb/success-summary id-path]
                               :op         (partial fetch/row-count
                                                    service
                                                    (assoc query :select [id-path]))}) id-paths)})))
(reg-event-fx
  :qb/export-query
  (fn [{db :db} [_]]
    {:db       db
     :dispatch [:results/set-query
                {:source :flymine-beta
                 :type   :query
                 :value  (make-query (get-in db loc))}]
     :navigate (str "results")}))


