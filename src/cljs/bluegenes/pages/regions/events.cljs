(ns bluegenes.pages.regions.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [imcljs.entity :as entity]
            [imcljs.fetch :as fetch]
            [clojure.string :as str]))

(defn parse-region [region-string]
  (let [parsed (into []
                     (remove
                      (fn [c] (some? (some #{c} #{":" ".." "-"})))
                      (str/split region-string (re-pattern "(:|\\.\\.|-)"))))]
    {:chromosome (nth parsed 0)
     :from (int (nth parsed 1))
     :to (int (nth parsed 2))}))

(reg-event-db
 :regions/save-results
 (fn [db [_ result-response]]
   (let [searched-for (get-in db [:regions :regions-searched])
         error (:error result-response)
         mapped-results (mapv (fn [region]
                                (assoc region :results
                                       (filterv (fn [{{:keys [start end locatedOn]} :chromosomeLocation}]
                                                  (and (= (:primaryIdentifier locatedOn) (:chromosome region))
                                                       ;; Overlaps the region specified in a search line.
                                                       (or (<= (:from region) (int start) (:to region))
                                                           (<= (:from region) (int end) (:to region))
                                                           (and (<= (int start) (:from region))
                                                                (<= (:to region) (int end))))))
                                                (:results result-response))))
                              searched-for)]
     (-> db
         (assoc-in [:regions :results] mapped-results)
         (assoc-in [:regions :loading] false)
         (assoc-in [:regions :error] error)))))

(reg-event-db
 :regions/set-selected-organism
 (fn [db [_ organism]]
   (assoc-in db [:regions :settings :organism] organism)))

(reg-event-db
 :regions/toggle-feature-type
 (fn [db [_ class]]
   (let [class-kw (keyword (:name class))
         m (get-in db [:mines (get db :current-mine) :service :model])
         descendants (keys (entity/extended-by m class-kw))
         status (get-in db [:regions :settings :feature-types class-kw])]
     (update-in db [:regions :settings :feature-types]
                merge
                (reduce (fn [total next]
                          (assoc total next (not status)))
                        {class-kw (not status)} descendants)))))

(reg-event-db
 :regions/select-all-feature-types
 (fn [db]
   (let [model (get-in db [:mines (get db :current-mine) :service :model])
         feature-types (entity/extended-by model :SequenceFeature)]
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
    (update query :where conj {:path "SequenceFeature"
                               :op "ISA"
                               :values types})
    query))

(defn add-organism-constraint [query short-name]
  (if short-name
    (update query :where conj {:path "SequenceFeature.organism.shortName"
                               :op "="
                               :value short-name})
    query))

(defn build-feature-query [regions]
  {:from "SequenceFeature"
   :select ["SequenceFeature.id"
            "SequenceFeature.name"
            "SequenceFeature.primaryIdentifier"
            "SequenceFeature.symbol"
            "SequenceFeature.chromosomeLocation.start"
            "SequenceFeature.chromosomeLocation.end"
            "SequenceFeature.chromosomeLocation.locatedOn.primaryIdentifier"]
   :where [{:path "SequenceFeature.chromosomeLocation"
            :op "OVERLAPS"
            :values (if (string? regions) [regions] (into [] regions))}]})

(reg-event-fx
 :regions/run-query
 (fn [{db :db} [_]]
   (let [to-search (str/split-lines (get-in db [:regions :to-search]))
         feature-types (get-in db [:regions :settings :feature-types])
         selected-feature-types (keep (fn [[feature-kw enabled?]]
                                        (when enabled? (name feature-kw)))
                                      feature-types)
         selected-organism (get-in db [:regions :settings :organism :shortName])
         query (-> (build-feature-query to-search)
                   (add-type-constraints selected-feature-types)
                   (add-organism-constraint selected-organism))
         subqueries (mapv (fn [region]
                            (-> (build-feature-query region)
                                (add-type-constraints selected-feature-types)
                                (add-organism-constraint selected-organism)))
                          to-search)]
     {:db (-> db
              (assoc-in [:regions :query] query)
              (assoc-in [:regions :subqueries] subqueries)
              (assoc-in [:regions :regions-searched] (mapv parse-region to-search))
              (assoc-in [:regions :results] [])
              (assoc-in [:regions :loading] true))
      :im-chan {:chan (fetch/records (get-in db [:mines (get db :current-mine) :service]) query)
                :on-success [:regions/save-results]}})))
