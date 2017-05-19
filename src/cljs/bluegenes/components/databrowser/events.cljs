(ns bluegenes.components.databrowser.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
;            [clojure.math.combinatorics :refer combinations]
            [bluegenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.counts :as counts]
            [accountant.core :refer [navigate!]]))

(def pi (.-PI js/Math))
(def viewport (subscribe [:databrowser/viewport]));; this wants to be dynamic when it grows up
(def margin 30) ;;marginize dem bubbles
(def padding 10) ;; pad dem bubbles
(defn center [] {:x (/ (:x @viewport) 2) :y (/ (:y @viewport) 2)})

(defn radius-from-count "like it sounds. strategy is to correlate the area to the log of the count, then whack it up in size a bit because we want to see these silly little dots." [count]
  (let [area (* (Math/log2 count) 100)
        r (Math/sqrt (/ area pi))]
(+ r padding)))


(defn random-coord [max-coord radius]
  (let [r (+ radius margin)
        x (* max-coord (.random js/Math))]
    (cond ;;these conds stop it from banging into the walls
      (< max-coord (+ r x))
        (- max-coord r)
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

(defn collides?
  "given two sets of circle coords, check if they collide at all.
  Essentially, the distance between their centres needs to be greater than
  the sum of their radiuses for this to be true. Also, a circle can't collide with itself."
  [c1 c2]
  (let [distance-between-centers (distance [(:x c1) (:y c1)] [(:x c2) (:y c2)])
        radius-sum (+ (:r c1) (:r c2))]
    (and (>= radius-sum distance-between-centers)
         (not= c1 c2))
))


(defn check-for-collision [mymap]
  "returns a list of colliding bubbles"
  (let [themap (vec mymap)]
    (doall (reduce (fn [new-map [id2 circle2]]
      (let [inner-layer (doall (reduce (fn [new-map2 [id3 circle3]]
        (if (collides? circle2 circle3)
          (assoc new-map2 id3 (assoc circle3 :collides-with circle2))
          new-map2))
       new-map themap))]
    inner-layer))
  {} themap))))



(defn build-central-node
  "The biggest node always goes smack in the centre"
  [sorted-counts]
  {(first (first sorted-counts))
    {:x (:x (center))
     :y (:y (center))
     :r (radius-from-count (second (first sorted-counts)))
     :name (first (first sorted-counts))}})

(defn assign-random-location
  "chooses a random location on the playing board and creats a coord & radius set for the circle"
  [k v]
  (let [radius (radius-from-count v)]
    {:x (random-coord (:x @viewport) radius)
     :y (random-coord (:y @viewport) radius)
     :r radius
     :name k}
  ))

(defn new-bubble
  "Generates a location for the bubble, checks if it's overlapping, and tries again if it is"
  ;;todo: this could cause an endless loop if there's just not enough room on the screen.
  ;;Maybe limit to x tries per bubble
  [k v locs]
  (let [possible-location (assign-random-location k v)
        collisions (check-for-collision (assoc locs k possible-location))]
    (if (empty? collisions)
      possible-location    ;;this is a good place to be a baby bubble.
    ;  possible-location
      (new-bubble k v locs);;try again, there are collisions
      )
))

(defn calculate-node-locations
  "Given counts, generate a coord set and sizes for the bubbles, and ensure that they don't overlap."
  [counts]
  (let [numeric-counts (map (fn [[a b]] [a (int b)]) counts) ;;they were strings
        sorted-counts (reverse (sort-by second numeric-counts)) ;;now they're ordered from large to small
        central-node (build-central-node sorted-counts)
        locs (reduce (fn [new-map [k v]]
          (assoc new-map k (new-bubble k v new-map))
    ) central-node (rest sorted-counts))]
  locs))

(reg-event-fx
  :databrowser/fetch-all-counts
  (fn [args [_ xy]]
    (let [db (:db args)]
    {:db (-> (assoc db :fetching-counts? true)
             (assoc :databrowser/viewport xy))
     :databrowser/fetch-counts {
      :connection
        {:root @(subscribe [:mine-url])}
      :path "top"
      } })))

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

(reg-event-db
 :databrowser/viewport
 (fn [db [_ xy]]
  (assoc db :databrowser/viewport xy)
))
