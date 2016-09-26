(ns redgenes.components.querybuilder.fx
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.filters :as filters]
            [redgenes.utils :refer [register!]]
            [redgenes.components.querybuilder.events :as events]
            [clojure.string :as string]))

#_(def im-zipper (zip/zipper
                   (fn branch? [node] true)
                   (fn children [node]
                     (println "raw" (:collections node))
                     (let [child-classes (map (comp child-classes second) (:collections node))]
                       (.log js/console "returning" (select-keys model child-classes))
                       (select-keys model child-classes)))
                   (fn make-node [node children]
                     (println "makde node called")
                     (assoc node :collections children))
                   (-> db :assets :model :Gene)))

(defn run-query!
   "Returns the x for the given y"
   {:reframe-kind :fx, :reframe-key :query-builder/run-query}
   [query]
   (go
     (dispatch
      [:query-builder/handle-count
       (<!
        (search/raw-query-rows
         {:root @(subscribe [:mine-url])}
         query
         {:format "count"}))])))

(doseq [v
    [
     #'redgenes.components.querybuilder.events/reset-query
     #'redgenes.components.querybuilder.events/add-constraint-cofx
     #'redgenes.components.querybuilder.events/change-constraint-value
     #'redgenes.components.querybuilder.events/handle-count
     #'redgenes.components.querybuilder.events/run-query-cofx
     #'redgenes.components.querybuilder.events/make-tree
     #'redgenes.components.querybuilder.events/remove-select-cofx
     #'redgenes.components.querybuilder.events/remove-constraint-cofx
     #'redgenes.components.querybuilder.events/add-filter
     #'redgenes.components.querybuilder.events/set-logic
     #'redgenes.components.querybuilder.events/set-query
     #'redgenes.components.querybuilder.events/update-io-query
     #'redgenes.components.querybuilder.events/add-view-cofx
     #'redgenes.components.querybuilder.events/set-where-path
     #'run-query!]]
  (register! v))