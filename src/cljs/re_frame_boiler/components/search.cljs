(ns re-frame-boiler.components.search
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))


(defn suggestion []
  (fn [item]
    [:div.list-group-item
     [:h4.list-group-item-heading (:type item)]
     [:p.list-group-item-text (interpose ", " (vals (:fields item)))]]))

(defn main []
  (reagent/create-class
    (let [results (subscribe [:suggestion-results])]
      {:component-did-mount (fn [e]
                              (let [node (js/$ (reagent/dom-node e))]
                                (-> node
                                    (.find "input")
                                    (.focus (fn [] (.addClass node "open")))
                                    (.blur (fn [] (.removeClass node "open"))))))
       :reagent-render
                            (fn []
                              [:div.dropdown
                               [:input.form-control.input-lg.square
                                {:data-toggle "collapse"
                                 :type        "text"
                                 :placeholder "Search"
                                 :on-change   #(dispatch [:bounce-search (-> % .-target .-value)])}]
                               (if @results
                                 [:div.dropdown-menu.full-width
                                  (into [:div.list-group] (map (fn [r] [suggestion r]) @results))])])})))
