(ns redgenes.components.querybuilder.views.constraints
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.components.lists.views :as list-views]
            [json-html.core :as json]))

(def ops ["=" "!=" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "CONTAINS" "ONE OF" "NONE OF"])

(def ops-for-type
  {
   "java.lang.Integer" #{"=" "!=" "<" "<=" ">" ">="}
   "java.lang.String" #{"=" "LIKE" "NOT LIKE" "CONTAINS" "ONE OF" "NONE OF"}
   })


(defn list-dropdown []
  (let [lists (subscribe [:lists])]
    (fn []
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        [:i.fa.fa-list.pad-right-5] "List"]
       (into [:ul.dropdown-menu] (map (fn [l]
                                        [:li [:a (str (:name l))]]) @lists))])))

(defn op []
  (let [state (reagent/atom {:q/op "="})]
    (fn [path tipe]
      [:div
       [:span (clojure.string/join " > " path)]
       [:div.input-group
        [:div.input-group-btn
         [:button.btn.btn-default.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          (:q/op @state)
          [:i.fa.fa-caret-down.pad-left-5]]
         (into [:ul.dropdown-menu]
               (map
                 (fn [op] [:li {:on-click (fn [] (swap! state assoc :q/op op))} [:a op]]))
           (or (ops-for-type tipe) ops))]
        [:input.form-control
         {:type      "text"
          :value     (:q/value @state)
          :on-change (fn [e] (swap! state assoc :q/value (.. e -target -value)))}]
        [:div.input-group-btn [list-dropdown]]]
       [:button.btn.btn-success
        {:on-click
         (fn []
           (dispatch
             [:query-builder/add-constraint
              (merge @state {:q/path path :tipe tipe})]))} "Add"]])))

(defn constraint []
  (fn [{:keys [path tipe]}]
    [:div
     [:h3 "Constraint"]
     [op path tipe]]))