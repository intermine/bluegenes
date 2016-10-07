(ns redgenes.components.databrowser.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
;            [clojure.math.combinatorics :refer combinations]
            [redgenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.counts :as counts]
            [accountant.core :refer [navigate!]]))

(def pi (.-PI js/Math))
(def viewport 500);; this wants to be dynamic when it grows up
(def padding 30);; pad dem bubbles

(defn radius-from-count "like it sounds. strategy is to correlate the area to the log of the count, then whack it up in size a bit because we want to see these silly little dots." [count]
  (let [area (* (Math/log2 count) 100)
        r (Math/sqrt (/ area pi))]
r))


(defn random-coord [max-coord radius]
  (let [r (+ radius padding)
        x (* max-coord (.random js/Math))]
    (cond ;;these conds stop it from banging into the walls
      (< max-coord (+ r x))
        (- r max-coord)
      (> r x)
        r
      :else x)
))

(defn distance
  "Euclidean distance between 2 points"
  [[x1 y1] [x2 y2]]
  (Math/sqrt
    (+ (Math/pow (- x1 x2) 2)
       (Math/pow (- y1 y2) 2))))

(defn collides? [c1 c2]
  (let [distance-between-centers (distance [(:x c1) (:y c1)] [(:x c2) (:y c2)])
        radius-sum (+ (:r c1) (:r c2))]
    ;(.log js/console "%cdistance-between-centers" "color:darkseagreen;" distance-between-centers radius-sum (>= radius-sum distance-between-centers))
    (and (>= radius-sum distance-between-centers)
         (not= c1 c2))
))


(defn check-for-collision [mymap]
  "returns a list of colliding bubbles"
  (let [themap (vec mymap)]
    (doall (reduce (fn [new-map [id2 circle2]]
      (let [inner-layer (doall (reduce (fn [new-map2 [id3 circle3]]
        (if (collides? circle2 circle3)
          (do
            (.log js/console "%cclash" "color:cornflowerblue;" id2 id3)
            (assoc new-map2 id3 (assoc circle3 :collides-with circle2)))
          new-map2)
      )
       new-map themap))]

       inner-layer))
  {} themap))))


(defn de-collide [[id coords] locations]
  ;(assoc-in locations )
  (.log js/console "XX" id coords)
  locations)

(defn resolve-collisions [locs]
  (let [collisions (check-for-collision locs)]
    (if (seq collisions)
      (do (.log js/console "%cCOLLISIONS:" "border-bottom:solid 3px red" collisions)
        (doall (map #(de-collide % locs) collisions))
        )
      (.log js/console "%cno collisions" "border-bottom:solid 3px darkseagreen"))
))

(defn calculate-node-locations [counts]
  (let [numeric-counts (map (fn [[a b]] [a (int b)]) counts) ;;they were strings
        sorted-counts (reverse (sort-by second numeric-counts)) ;;now they're ordered from large to small
        center (/ viewport 2)
        central-node {(first (first sorted-counts))
          {:x center
           :y center
           :r (radius-from-count (second (first sorted-counts)))
           :name (first (first sorted-counts))}}
        locs (reduce (fn [new-map [k v]]
          (let [radius (radius-from-count v)]
          (assoc new-map k
           {:x (random-coord viewport radius)
            :y (random-coord viewport radius)
            :r radius
            :name k}))
    ) central-node (rest sorted-counts))]

    (resolve-collisions locs)
    (.log js/console "%cCollisions" "border-bottom:solid 1px green;" )

    locs))

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
