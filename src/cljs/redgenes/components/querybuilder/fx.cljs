(ns redgenes.components.querybuilder.fx
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.filters :as filters]
            [redgenes.utils :refer [register!]]
            [redgenes.components.querybuilder.events :as events]
            [clojure.string :as string]
            [cljs.spec :as spec]))

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
   "Runs the given query, returns a channel"
   {:reframe-kind :fx, :reframe-key :query-builder/run-query!}
   [query]
   (go
     (dispatch
      [:query-builder/handle-count
       (<!
        (search/raw-query-rows
         {:root @(subscribe [:mine-url])}
         query
         {:format "count"}))])))

(defn maybe-run-query!
  "Maybe runs the given query"
  {:reframe-kind :fx, :reframe-key :query-builder/maybe-run-query!}
  [{query :query query? :query?}]
    (cond (and query? (spec/valid? :q/query query)) (run-query! query)))

(doseq
  [v
    [
     #'redgenes.components.querybuilder.events/reset-query
     #'redgenes.components.querybuilder.events/add-constraint-cofx
     #'redgenes.components.querybuilder.events/change-constraint-value
     #'redgenes.components.querybuilder.events/change-constraint-op
     #'redgenes.components.querybuilder.events/handle-count
     #'redgenes.components.querybuilder.events/run-query-cofx
     #'redgenes.components.querybuilder.events/make-tree
     #'redgenes.components.querybuilder.events/remove-select-cofx
     #'redgenes.components.querybuilder.events/remove-constraint-cofx
     #'redgenes.components.querybuilder.events/add-filter
     #'redgenes.components.querybuilder.events/set-logic
     #'redgenes.components.querybuilder.events/set-logic-cofx
     #'redgenes.components.querybuilder.events/set-query
     #'redgenes.components.querybuilder.events/toggle-autoupdate
     #'redgenes.components.querybuilder.events/update-io-query
     #'redgenes.components.querybuilder.events/toggle-view-cofx
     #'redgenes.components.querybuilder.events/maybe-run-query-cofx
     #'redgenes.components.querybuilder.events/set-where-path
     #'run-query!
     #'maybe-run-query!
     ]]
  (register! v))