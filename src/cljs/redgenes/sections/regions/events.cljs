(ns redgenes.sections.regions.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.model :as m]
            [imcljs.search :as search]
            [clojure.spec :as s]
            [day8.re-frame.http-fx]
            [redgenes.events]
            [ajax.core :as ajax]))


(defn parse-region [region-string]
  (let [parsed (into []
                     (remove (fn [c]
                               (some? (some #{c} #{":" ".."})))
                             (clojure.string/split region-string (re-pattern "(:|\\..)"))))]
    {:chromosome (nth parsed 0)
     :from       (int (nth parsed 1))
     :to         (int (nth parsed 2))}))

(reg-event-db
  :regions/save-results
  (fn [db [_ result-response]]
    (let [searched-for   (get-in db [:regions :regions-searched])
          mapped-results (map (fn [region]
                                (assoc region :results (filter (fn [{{:keys [start end locatedOn] :as t} :chromosomeLocation}]
                                                                 (and
                                                                   (= (:primaryIdentifier locatedOn) (:chromosome region))
                                                                   (or
                                                                     (< (:from region) (int start) (:to region))
                                                                     (< (:from region) (int end) (:to region)))))
                                                               (:results result-response)))
                                ) searched-for)]
      (assoc-in db [:regions :results] mapped-results))))


(reg-event-db
  :regions/set-selected-organism
  (fn [db [_ organism]]
    (assoc-in db [:regions :settings :organism] organism)))


(reg-event-db
  :regions/toggle-feature-type
  (fn [db [_ class-name]]
    (update-in db [:regions :settings :feature-types class-name] not)))

(reg-event-db
  :regions/select-all-feature-types
  (fn [db]
    (let [model         (get-in db [:assets :model])
          feature-types (m/descendants-of model :SequenceFeature)]
      (assoc-in db [:regions :settings :feature-types]
                (reduce (fn [total [name]]
                          (assoc total (keyword name) true)) {} feature-types)))))

(reg-event-db
  :regions/deselect-all-feature-types
  (fn [db]
    (assoc-in db [:regions :settings :feature-types] nil)))

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

(reg-event-fx
  :regions/run-query
  (fn [{db :db} [_]]
    (let [to-search     (clojure.string/split-lines (get-in db [:regions :to-search]))
          feature-types (get-in db [:regions :settings :feature-types])
          query         (add-type-constraints
                          (search/build-feature-query to-search)
                          (map name (keys (filter (fn [[name enabled?]] enabled?) feature-types))))]
      {:db           (assoc-in db [:regions :regions-searched] (map parse-region to-search))
       :im-operation {:op         (partial search/raw-query-rows
                                           {:root @(subscribe [:mine-url])}
                                           query
                                           {:format "jsonobjects"})
                      :on-success [:regions/save-results]}})))