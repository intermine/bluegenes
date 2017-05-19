(ns bluegenes.sections.saveddata.views.venn
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [bluegenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [cljs-time.core :as t]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]
            [clojure.string :refer [join]]))


(defn circle-intersections
  "Determines the pair of X and Y coordinates where two circles intersect."
  [x0 y0 r0 x1 y1 r1]
  (let [dx       (- x1 x0)
        dy       (- y1 y0)
        d        (.sqrt js/Math (+ (* dy dy) (* dx dx)))
        a        (/ (+ (- (* r0 r0) (* r1 r1)) (* d d)) (* 2 d))
        x2       (+ x0 (* dx (/ a d)))
        y2       (+ y0 (* dy (/ a d)))
        h        (.sqrt js/Math (- (* r0 r0) (* a a)))
        rx       (* (* -1 dy) (/ h d))
        ry       (* dx (/ h d))
        xi       (+ x2 rx)
        xi-prime (- x2 rx)
        yi       (+ y2 ry)
        yi-prime (- y2 ry)]
    [xi xi-prime yi yi-prime]))

(def width 300)
(def circle-width 300)
(def height 200)
(def bracket-width 10)
(def bracket-padding 10)

(def anchor1 {:x (- (* .33 circle-width) (/ width 2)) :y 0})
(def anchor2 {:x (- (* .66 circle-width) (/ width 2)) :y 0})
(def radius (* .25 (- circle-width (* 2 bracket-width))))


(defn overlap []
  (let [selected?           (subscribe [:saved-data/merge-intersection])
        intersection-points (circle-intersections (:x anchor1) 0 radius (:x anchor2) 0 radius)]
    (fn []
      [:path.part
       {:class    (if @selected? "selected")
        :d        (clojure.string/join
                    " "
                    ["M" (nth intersection-points 1) (nth intersection-points 3)
                     "A" radius radius 0 0 1
                     (nth intersection-points 0) (nth intersection-points 2)
                     "A" radius radius 0 0 1
                     (nth intersection-points 1) (nth intersection-points 3)
                     "Z"])
        :on-click (fn [] (dispatch [:saved-data/toggle-keep-intersections]))}])))

(defn left-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" 0 bracket-padding
                           "L" bracket-width bracket-padding
                           "L" bracket-width (- height bracket-padding)
                           "L" 0 (- height bracket-padding)])}]))

(defn right-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" circle-width bracket-padding
                           "L" (- circle-width bracket-width) bracket-padding
                           "L" (- circle-width bracket-width) (- height bracket-padding)
                           "L" circle-width (- height bracket-padding)])}]))

(defn top-bracket []
  (fn []
    [:path {:stroke "#808080"
            :fill   "transparent"
            :d      (join " "
                          ["M" (* 2 bracket-padding) 0
                           "L" (* 2 bracket-padding) bracket-padding
                           "L" (- circle-width (* 2 bracket-padding)) bracket-padding
                           "L" (- circle-width (* 2 bracket-padding)) 0
                           ])}]))

(defn circ []
  (let []
    (fn [{:keys [type path keep id]}]
      [:g
       [:circle.part
        {:class    (if (:self keep) "selected")
         :r        radius
         :on-click (fn [x] (dispatch [:saved-data/toggle-keep id]))}]
       #_[:text {:text-anchor "end"
                 :x           50} (str path)]])))

(defn main []
  (let [editable-ids (subscribe [:saved-data/editable-ids])]
    (fn []
      (let [[item-1 item-2] (take 2 @editable-ids)]
        [:svg.venn {:width width :height height}
         [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
          [:g {:transform (str "translate(" (:x anchor1) ",0)")} [circ item-1]]
          [:g {:transform (str "translate(" (:x anchor2) ",0)")} [circ item-2]]
          [overlap]]
         [left-bracket]
         [right-bracket]
         ;[top-bracket]
         ]))))
