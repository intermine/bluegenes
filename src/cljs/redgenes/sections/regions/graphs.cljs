(ns redgenes.sections.regions.graphs
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.regions.events]
            [redgenes.sections.regions.subs]
            [redgenes.components.imcontrols.views :as im-controls]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]))

(defn linear-scale [[r1-lower r1-upper] [r2-lower r2-upper]]
  (fn [v]
    (+ (/ (* (- v r1-lower) (- r2-upper r2-lower))
          (- r1-upper r1-lower))
       r2-lower)))

(defn datum []
  (fn [scale {{:keys [start end]} :chromosomeLocation}]
    [:rect.datum
     {:x      (scale start)
      :y      20
      :width  (scale end)
      :height 60
      :filter "url(#blurMe)"}]))

(defn bar []
  (fn [scale loc]
    [:g
     [:path.guide
      {:stroke-width 0.002
       :d            (clojure.string/join
                       " "
                       ["M" (scale loc) 85
                        "L" (scale loc) 100])}]]))

(defn svg []
  (fn [scale-fn data-points to from]
    [:svg.region
     {:width                 "100%"
      :height                "60px"
      :preserve-aspect-ratio "none"
      :view-box              "0 0 1 100"}
     [:filter#blurMe
      [:feGaussianBlur
       {:in           "SourceGraphic"
        :stdDeviation 0.2}]]
     (into [:g] (map (fn [d] [datum scale-fn d]) (sort-by (comp :end :chromosomeLocation) data-points)))
     [bar scale-fn from]
     [bar scale-fn to]]))

(defn main []
  (fn [{:keys [results to from]}]
    (let [min-start (apply min (map (comp :start :chromosomeLocation) results))
          max-end   (apply max (map (comp :end :chromosomeLocation) results))
          scale     (linear-scale [min-start max-end] [0 1])]
      [:div
       [:span "Distribution"]
       [svg scale results to from]])))
