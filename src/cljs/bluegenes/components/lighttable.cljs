(ns bluegenes.components.lighttable
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [imcljsold.names :refer [find-name]]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.search :as search]
            [imcljs.fetch :as fetch]))


(defn homogeneous-columns
  "Returns a sequence of true / false indicating that all values in each
  column of a table are equal. Assumes all rows are the same length.
  [[A B C] [X B Y]]
   => (false true false)"
  [table]
  (map (fn [column] (apply = (map (fn [row] (nth row column)) table))) (range (count (first table)))))


(defn table
  "a basic results table without imtables complexity. optional second arg options allows you to specify whether or not to show a title for the table, as {:title true}. "
  []
  (fn
    ([results]
     (let [skip-columns nil #_(homogeneous-columns (:results results))]
       [:table.table.small
        [:thead
         (into [:tr]
               (map (fn [header]
                      [:th (last (clojure.string/split header " > "))]) (:columnHeaders results)))]
        (into [:tbody]
              (map (fn [row]
                     (into [:tr]
                           (map-indexed (fn [idx value]
                                          (if (nth skip-columns idx)
                                            [:td.skipped "..."]
                                            [:td (if (< 50 (count (str value)))
                                                   (str (apply str (take 50 (str value))) "...")
                                                   (str value))])) row))) (:results results)))]))

    ([results options]
     (if (:title options) ;; we could tweak this further to make a nice passed-in title, too
       [:div
        [:h4 (find-name (:class results))]
        [table results]]
       [table results])
      )))

;;;;;
;(find-name (:class query))
;;;

(defn shell []
  (fn [state package options]
    (if (empty? (:results @state))
      [:div.small.no-results (str (:class @state) " - No Results")]
      [:div.lt [table @state options]])))

(defn handler [state e]
  (let [props (reagent/props e)
        node  (sel1 (reagent/dom-node e) :.im-target)
        missing-values (filter #(and
                                  (= nil (:value %))
                                  (some? (:op %))) (:where (:query props)))]
    (if (empty? missing-values)
      (go (let [new-results (<! (fetch/rows
                                 (:service props)
                                 (:query props)
                                 {:size   5
                                  :format "json"}))]
           ;; assoc the original class so we can say what has no results
           (reset! state (assoc new-results :class (:class props)))
           )))))

(defn main []
  (let [state (reagent/atom nil)]
    (reagent/create-class
      {:component-did-mount  (partial handler state)
       :component-did-update (partial handler state)
       :reagent-render       (fn [package options]
                               [shell state package options])})))
