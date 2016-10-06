(ns redgenes.components.databrowser.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.counts :as counts]
            [accountant.core :refer [navigate!]]))

(def pi (.-PI js/Math))
(def viewport 500);; this wants to be dynamic when it grows up
(def padding 20);; pad dem bubbles

(defn radius-from-count "like it sounds. strategy is to correlate the area to the log of the count, then whack it up in size a bit because we want to see these silly little dots." [count]
  (let [area (* (Math/log2 count) 100)
        r (Math/sqrt (/ area pi))]
r))

(defn random-coord [max-coord radius]
  (let [r (+ radius padding)
        x (* max-coord (.random js/Math))]
    (cond
      (< max-coord (+ r x))
        (- r max-coord)
      (> r x)
        r
      :else x)
))

(defn calculate-node-locations [counts]

  (let [numeric-counts (map (fn [[a b]] [a (int b)]) counts)
        ;; sorted-counts format [[:Homologue 25] [:Protein 99]]
        sorted-counts (reverse (sort-by second numeric-counts))
        center (/ viewport 2)
        central-node {(first (first sorted-counts)) {:x center :y center :r (radius-from-count (second (first sorted-counts)))}}]
    (reduce (fn [new-map [k v]]
      (let [radius (radius-from-count v)]
      (assoc new-map k {:x (random-coord 500 radius) :y (random-coord 500 radius) :r radius}))
    ) central-node (rest sorted-counts))

    ;; first one is the center, then use the radius of circle one and two to place the first circle straight to the right.
    ;; then calculate angles for some more and seeeeeee.
  ))

(reg-event-fx
  :databrowser/fetch-all-counts
  (fn [{db :db}]
    {:db (assoc db :fetching-counts? true)
     :databrowser/fetch-counts {
      :connection
        {:root @(subscribe [:mine-url])}
      :path "top"
      } }))


(reg-fx
  :databrowser/fetch-counts
  (fn [{connection :connection path :path}]
      (go (let [res (<! (counts/count-rows connection path))]
            (re-frame/dispatch [:databrowser/save-counts :human res])
      ))
))

(reg-event-db
 :databrowser/save-counts
 (fn [db [_ mine-name counts]]
   (->
     (assoc-in db [:databrowser/model-counts mine-name] counts)
     (assoc :fetching-counts? false)
     (assoc :databrowser/node-locations (calculate-node-locations counts))
   )
))
