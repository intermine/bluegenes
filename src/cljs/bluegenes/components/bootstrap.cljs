(ns bluegenes.components.bootstrap
  (:require [reagent.core :as reagent]
            [oops.core :refer [ocall oapply oget oset!]]))


(defn popover
  "Reagent wrapper for bootstrap's popover component. It accepts
  hiccup-style syntax in its :data-content attribute.
  Usage:
  [popover [:li {:data-trigger hover
                 :data-placement right
                 :data-content [:div [:h1 Hello] [:h4 Goodbye]]} Hover-Over-Me]]"
  []
  (reagent/create-class
    {:component-did-mount
     (fn [this]
       (let [node (reagent/dom-node this)] (ocall (-> node js/$) "popover")))
     :component-will-unmount
     (fn [this]
       (let [node (reagent/dom-node this)] (ocall (-> "popover" js/$) "remove")))
     :reagent-render
     (fn [[element attributes & rest]]
       [element (-> attributes
                    (assoc :data-html true)
                    (assoc :data-container "body")
                    (update :data-content reagent/render-to-static-markup)) rest])}))

(defn tooltip
  "Reagent wrapper for bootstrap's tooltip component.
  Usage:
  [tooltip [:i.fa.fa-question {:data-trigger hover
                               :data-placement right
                               :title Some string here]"
  []
  (reagent/create-class
    {:component-did-mount
     (fn [this]
       (let [node (reagent/dom-node this)] (ocall (-> node js/$) "tooltip")))
     :reagent-render
     (fn [[element attributes & rest]] [element attributes rest])}))

(defn tooltip-new
  "Reagent wrapper for bootstrap's tooltip component.
  Usage:
  [tooltip [:i.fa.fa-question {:data-trigger hover
                               :data-placement right
                               :title Some string here]"
  []
  (reagent/create-class
    {:component-did-mount
     (fn [this]
       (let [node (reagent/dom-node this)]
         (ocall (-> node js/$) "tooltip")))
     :reagent-render
     (fn [props & [contents]]
       [:span props contents])}))




