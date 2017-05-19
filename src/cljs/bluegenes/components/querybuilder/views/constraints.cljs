(ns bluegenes.components.querybuilder.views.constraints
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.components.querybuilder.core :refer [ops]]
            [json-html.core :as json]))

(def ops-for-type
  {
   "java.lang.Integer" #{"=" "!=" "<" "<=" ">" ">="}
   "java.lang.String" #{"=" "LIKE" "NOT LIKE" "CONTAINS" "ONE OF" "NONE OF"}
   })

(def type-for-type
  {
   "java.lang.Integer" :number
   "java.lang.String" :text
   })

(def pattern-for-type
  {
   "java.lang.Integer" "[0-9]+"
   "java.lang.String" "[a-zA-Z0-9]*"
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
    (fn [path typ]
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
           (or (ops-for-type typ) ops))]
        [:input.form-control
         {:type      (type-for-type typ)
          :value     (:q/value @state)
          :on-change (fn [e] (swap! state assoc :q/value (.. e -target -value)))}]
        [:div.input-group-btn [list-dropdown]]]
       [:button.btn.btn-success
        {:on-click
         (fn []
           (dispatch
             [:query-builder/add-constraint!
              (merge @state {:q/path path :typ typ})]))} "Add"]])))

(defn constraint []
  (fn [{:keys [path typ]}]
    [:div
     [op path typ]]))
