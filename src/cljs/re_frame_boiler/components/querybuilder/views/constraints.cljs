(ns re-frame-boiler.components.querybuilder.views.constraints
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.lists.views :as list-views]
            [json-html.core :as json]))

(def ops
  ["=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"])

(defn op []
  (let [state (reagent/atom "=")]
    (fn [path]
      [:div
       [:div (str path)]
       [:div.input-group
        [:div.input-group-btn
         [:button.btn.btn-default.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          @state]
         (into [:ul.dropdown-menu]
               (map (fn [op] [:li {:on-click (partial reset! state op)} [:a op]])) ops)]
        [:input.form-control {:type "text"}]
        [:div.input-group-btn
         [:button.btn.btn-primary {:type "button"} [:i.fa.fa-list.pad-right-5] "List"]]]])))

(defn constraint []
  (fn [path]
    [:div
     [:h3 "Constraint"]
     [op path]]))