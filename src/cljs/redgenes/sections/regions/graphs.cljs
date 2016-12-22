(ns redgenes.sections.regions.graphs
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.regions.events]
            [redgenes.sections.regions.subs]
            [redgenes.components.imcontrols.views :as im-controls]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget oset!]]))

(defn linear-scale [[r1-lower r1-upper] [r2-lower r2-upper]]
  (fn [v]
    (+ (/ (* (- v r1-lower) (- r2-upper r2-lower))
          (- r1-upper r1-lower))
       r2-lower)))

(defn datum []
  (fn [scale {{:keys [start end]} :chromosomeLocation}]
    [:rect.datum
     {:x      (scale start)
      :y      0
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
                       ["M" (scale loc) 0
                        "L" (scale loc) 75])}]
     ]))

     (defn label-text [scale text width properties]
       [:div.layout [:div.label-text (merge properties
       {:style {:left (* width (scale text))}}) [:span.value text]]])

(defn svg []
  (fn [scale-fn data-points to from min max]
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
     [bar scale-fn to]
     [bar scale-fn min]
     [bar scale-fn max]
     ]))

(defn resize-handler [atom this] (reset! atom (.-clientWidth (reagent/dom-node this))))

(defn main []
  (let [width (reagent/atom nil)]

  (reagent/create-class
   {:reagent-render
     (fn [{:keys [results to from]} x]
       (let [min-start (apply min (map (comp :start :chromosomeLocation) results))
          max-end   (apply max (map (comp :end :chromosomeLocation) results))
          scale     (linear-scale [min-start max-end] [0 1])]
          [:div.graph
            [svg scale results to from min-start max-end]
            [:span.distribution "Distribution"]
            [:div.legend
             [:span.start {:title "Starting location of the earliest overlaping feature(s)"} min-start]
             [:span.end {:title "End location of the last overlapping feature"} max-end]]
            [label-text scale from @width {:class "from" :title "This is the start location you input in the box above"}]
            [label-text scale to @width {:class "to" :title "This is the end location you input into the box above"}]
       ]))
      :component-did-mount
        (fn [this]
          (.addEventListener js/window "resize" #(resize-handler width this))
          (resize-handler width this))
      :component-did-update (fn [this] (resize-handler width this))
      :component-will-unmount #(.removeEventListener js/window "resize" resize-handler)
    })))
