(ns bluegenes.components.bootstrap
  (:require [reagent.core :as reagent]
            [reagent.dom.server :refer [render-to-static-markup]]
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
                    (update :data-content render-to-static-markup)) rest])}))

(defn tooltip-new
  ;;DEPRECATED REPLACE WITH tooltip when encountered.
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

 (defn tooltip
   "Reagent wrapper for bootstrap's tooltip component.
   Usage:
   [tooltip {:title  \"Your fabulous tooltip content goes here, m'dear\"}
    ;; svg below is the clickable content - e.g. an icon and/or words to go
    ;; along with it. Implemented as the 'remaining' arg below.
    [:svg.icon.icon-question
     [:use {:xlinkHref \"#icon-question\"}]]]"
   []
   (let [dom (reagent/atom nil)]
     (reagent/create-class
       {:name "Tooltip"
        :reagent-render (fn [props & [remaining]]
                          [:a
                           (merge {:data-trigger "hover"
                                   :data-html true
                                   :data-toggle "tooltip"
                                   :data-container "body"
                                   :title nil
                                   :data-placement "auto bottom"
                                   :ref (fn [el] (some->> el js/$ (reset! dom)))}
                                  props)
                           remaining])})))
