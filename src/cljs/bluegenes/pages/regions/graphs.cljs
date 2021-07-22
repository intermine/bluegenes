(ns bluegenes.pages.regions.graphs
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.table :as table]
            [bluegenes.pages.regions.events]
            [bluegenes.pages.regions.subs]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget ocall]]))

(defn linear-scale [[r1-lower r1-upper] [r2-lower r2-upper] [bound-lower bound-upper]]
  (fn [v]
    (-> (+ (/ (* (- v r1-lower) (- r2-upper r2-lower))
              (- r1-upper r1-lower))
           r2-lower)
        ;; Ensure it "sticks" to the boundaries instead of exceeding them.
        (max bound-lower)
        (min bound-upper))))

(defn datum []
  (fn [idx scale {{:keys [start end locatedOn]} :chromosomeLocation} features-count]
    (let [highlighted? @(subscribe [:regions/highlighted? idx
                                    {:chromosome (:primaryIdentifier locatedOn)
                                     :start start :end end}])]
      [:rect.datum
       {:x      (scale start)
        :y      0
        :width  (max 0.001 ; Ensure a feature can never be so small it isn't visible.
                     (- (scale end) (scale start)))
        :height 60
        :style {:opacity (max 0.05 ;; Opacity less than this is barely visible.
                              (/ 1 features-count))}
        :class (when highlighted? :highlighted)}])))

(defn bar []
  (fn [scale loc]
    [:path.guide
     {:stroke-width 0.002
      :d            (clojure.string/join
                     " "
                     ["M" (scale loc) 0
                      "L" (scale loc) 75])}]))

(defn label-text [scale text width properties]
  [:div.layout
   [:div.label-text
    (merge properties
           {:style {:left (* width (scale text))}})
    [:span.value text]]])

(defn svg []
  (fn [idx scale-fn data-points to from min max]
    [:svg.region
     {:width                 "100%"
      :height                "60px"
      :preserve-aspect-ratio "none"
      :view-box              "0 0 1 100"}
     (into [:g]
           (map (fn [d]
                  [datum idx scale-fn d (count data-points)])
                ;; Render regions based on size, so smaller regions are placed
                ;; on top of larger regions, making them more visible.
                (sort-by (fn [{{:keys [start end]} :chromosomeLocation}]
                           (- start end))
                         data-points)))
     [bar scale-fn from]
     [bar scale-fn to]
     [bar scale-fn min]
     [bar scale-fn max]]))

(defn legend-width [width]
  (reset! width
          (.-clientWidth (.querySelector js/document ".legend"))))

(defn main []
  (let [width (reagent/atom 1000)] ;;1000 is completely arbitrary. could equally be 0.
    (reagent/create-class
     {:reagent-render
      (fn [idx {:keys [results to from]}]
        (reagent/with-let
          [min-start from
           max-end   to
           scale     (linear-scale [min-start max-end] [0.01 0.99] [0 1])]
           ; handler   #(legend-width width)
           ; listener  (.addEventListener js/window "resize" handler)]
          [:div.graph
           [svg idx scale results to from min-start max-end]
           #_[:div.legend
              [:span.start
               {:title (str min-start " \n"
                            "Starting location of the earliest overlapping feature(s)")} min-start]
              [:span.end
               {:title (str max-end " \n"
                            "End location of the last overlapping feature(s)")} max-end]]
           #_[label-text scale from @width
              {:class "from"
               :title (str from " \n"
                           "This is the start location you input into the box above")}]
           #_[label-text scale to @width
              {:class "to"
               :title (str to " \n"
                           "This is the end location you input into the box above")}]
           ;; The commented out forms have been replaced by a simpler solution
           ;; below, in case we want to return to the old solution. See paired
           ;; comment in Less code.
           [:div.labels
            [:span.from
             {:title (str from " \n"
                          "This is the start location you input into the box above")}
             from]
            [:span.to
             {:title (str to " \n"
                          "This is the end location you input into the box above")}
             to]]]
          #_(finally
              (.removeEventListener js/window "resize" handler))))})))
      ; :component-did-mount #(legend-width width)})))
