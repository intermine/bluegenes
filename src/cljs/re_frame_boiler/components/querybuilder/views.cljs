(ns re-frame-boiler.components.querybuilder.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [clojure.zip :as zip]
            [json-html.core :as json]))


(defn attribute []
  (let [state (reagent/atom false)]
    (fn [name]
      [:div
       {:class    (if @state "active")
        :on-click (fn [] (swap! state (fn [v] (not v))))}
       name
       (if @state
         [:span
          [:i.fa.fa-eye.pad-left]
          [:i.fa.fa-filter.pad-left]])])))

(defn tree [class & [open?]]
  (let [model (subscribe [:model])
        open  (reagent/atom open?)]
    (fn [class]
      ;(println "model" model)
      [:li
       [:div {:on-click (fn [] (swap! open (fn [v] (not v))))}
        (if @open
          [:i.fa.fa-minus-square.pad-right]
          [:i.fa.fa-plus-square.pad-right]) class]
       (if @open (into [:ul]
                       (concat
                         (map (fn [[_ details]]
                                [:li.leaf [attribute (:name details)]]) (sort (-> @model class :attributes)))
                         (map (fn [[_ details]]
                                [tree (keyword (:referencedType details))]) (sort (-> @model class :collections))))))])))

(defn main []
  (let [model-tree (subscribe [:model-tree])]
    (fn [x]
      [:div.querybuilder
       [:div.row
        [:div.col-sm-6
         [:div.panel
          [:h4 "Data Model"]
          [:ol.tree [tree :Gene true]]]]
        [:div.col-sm-6
         [:span (json/edn->hiccup @model-tree)]
         [:div.panel
          [:h4 "query"]]]]])))


