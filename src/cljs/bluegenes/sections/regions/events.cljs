(ns bluegenes.sections.regions.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [imcljsold.model :as m]
            [imcljs.fetch :as fetch]))


(defn parse-region [region-string]
  (let [parsed (into []
    (remove
      (fn [c] (some? (some #{c} #{":" ".." "-"})))
      (clojure.string/split region-string (re-pattern "(:|\\.\\.|-)"))))]
    {:chromosome (nth parsed 0)
     :from       (int (nth parsed 1))
     :to         (int (nth parsed 2))}))

(reg-event-db
  :regions/save-results
  (fn [db [_ result-response]]
    (let [searched-for   (get-in db [:regions :regions-searched])
          error          (:error result-response)
          mapped-results (map
            (fn [region]
                  (assoc region :results (filter (fn [{{:keys [start end locatedOn] :as t} :chromosomeLocation}]
                   (and
                     (= (:primaryIdentifier locatedOn) (:chromosome region))
                     (or
                       (< (:from region) (int start) (:to region))
                       (< (:from region) (int end) (:to region)))))
                 (:results result-response)))
            ) searched-for)]
      (->
       (assoc-in db [:regions :results] mapped-results)
       (assoc-in    [:regions :loading] false)
       (assoc-in    [:regions :error]   error)))))


(reg-event-db
  :regions/set-selected-organism
  (fn [db [_ organism]]
    (assoc-in db [:regions :settings :organism] organism)))


(reg-event-db
  :regions/toggle-feature-type
  (fn [db [_ class]]
    (let [class-kw    (keyword (:name class))
          m           (get-in db [:mines (get db :current-mine) :service :model :classes])
          descendants (keys (m/descendant-classes m class-kw))
          status      (get-in db [:regions :settings :feature-types class-kw])]
      (update-in db [:regions :settings :feature-types]
                 merge
                 (reduce (fn [total next]
                           (assoc total next (not status)))
                         {class-kw (not status)} descendants)))))

(reg-event-db
  :regions/select-all-feature-types
  (fn [db]
    (let [model         (get-in db [:mines (get db :current-mine) :service :model :classes])
          feature-types (m/descendant-classes model :SequenceFeature)]
      (assoc-in db [:regions :settings :feature-types]
                (reduce (fn [total [name]]
                          (assoc total (keyword name) true)) {} feature-types)))))

(reg-event-db
  :regions/deselect-all-feature-types
  (fn [db]
    (let [feature-types (get-in db [:regions :settings :feature-types])]
    (assoc-in db [:regions :settings :feature-types]
      (reduce (fn [new-map [k v]]
                (assoc new-map k false)) {} feature-types)))))

(reg-event-db
  :regions/set-to-search
  (fn [db [_ val]]
    (assoc-in db [:regions :to-search] val)))

(defn add-type-constraints [query types]
  (if types
    (update query :where conj {:path   "SequenceFeature"
                               :op     "ISA"
                               :values types})
    query))

(defn add-organism-constraint [query short-name]
  (if short-name
    (update query :where conj {:path   "SequenceFeature.organism.shortName"
                               :op     "="
                               :value short-name})
    query))


(defn build-feature-query [regions]
  {:from   "SequenceFeature"
   :select ["SequenceFeature.id"
            "SequenceFeature.name"
            "SequenceFeature.primaryIdentifier"
            "SequenceFeature.symbol"
            "SequenceFeature.chromosomeLocation.start"
            "SequenceFeature.chromosomeLocation.end"
            "SequenceFeature.chromosomeLocation.locatedOn.primaryIdentifier"]
   :where  [{:path   "SequenceFeature.chromosomeLocation"
             :op     "OVERLAPS"
             :values (if (string? regions) [regions] (into [] regions))}]})


(reg-event-fx
  :regions/run-query
  (fn [{db :db} [_]]
    (let [to-search     (clojure.string/split-lines (get-in db [:regions :to-search]))
          feature-types (get-in db [:regions :settings :feature-types])
          selected-organism (get-in db [:regions :settings :organism :shortName])
          query (->
                  (add-type-constraints
                   (build-feature-query to-search)
                   (map name (keys (filter (fn [[name enabled?]] enabled?) feature-types))))
                  (add-organism-constraint selected-organism))]
      {:db           (->
                      (assoc-in db [:regions :regions-searched] (map parse-region to-search))
                      (assoc-in [:regions :results] {})
                      (assoc-in [:regions :loading] true))
       :im-operation {:op         (partial fetch/records
                                           (get-in db [:mines (get db :current-mine) :service])
                                           query)
                      :on-success [:regions/save-results]}})))
