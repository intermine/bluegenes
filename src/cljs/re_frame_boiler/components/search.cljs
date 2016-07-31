(ns re-frame-boiler.components.search
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]))


(defn suggestion []
  (fn [item]
    [:div.list-group-item
     [:h4.list-group-item-heading (:type item)]
     [:p.list-group-item-text (interpose ", " (vals (:fields item)))]]))

(defn main []
  (let [results (subscribe [:suggestion-results])]
    (fn []
      [:div.dropdown
       [:input.form-control.input-lg
        {:data-toggle "dropdown"
         :type        "text"
         :placeholder "Search"
         :on-change   #(dispatch [:bounce-search (-> % .-target .-value)])}]
       (if @results
         [:div.dropdown-menu.full-width
         (into [:div.list-group] (map (fn [r] [suggestion r]) @results))])])))
