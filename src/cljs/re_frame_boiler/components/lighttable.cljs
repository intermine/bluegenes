(ns re-frame-boiler.components.lighttable
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]))


(defn homogeneous-columns
  "Returns a sequence of true / false indicating that all values in each
  column of a table are equal. Assumes all rows are the same length.
  [[A B C] [X B Y]]
   => (false true false)"
  [table]
  (map (fn [column] (apply = (map (fn [row] (nth row column)) table))) (range (count (first table)))))


(defn table []
  (fn [results]
    (let [skip-columns (homogeneous-columns (:results results))]
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
                                                  (str value))])) row))) (:results results)))])))

(defn handler [state e]
  (let [props (reagent/props e)
        node  (sel1 (reagent/dom-node e) :.im-target)]
    (go (reset! state (<! (search/raw-query-rows {:root "www.flymine.org/query"}
                                                 (:query props)
                                                 {:size   5
                                                  :format "json"}))))))

(defn main []
  (let [state (reagent/atom nil)]
    (reagent/create-class
      {:component-did-mount (partial handler state)
       :reagent-render      (fn [props]
                              [:div
                               (if (empty? (:results @state))
                                 [:div.small (str (:class props) " - No Results")]
                                 [:div
                                  [:span (str (:class props) " (" (count (:results @state)) ")")]
                                  [table @state]])])})))