(ns redgenes.components.ui.constraint
  (:require [imcljs.path :as im-path]
            [oops.core :refer [oget]]
            [reagent.core :as reagent :refer [create-class]]
            [redgenes.components.ui.list_dropdown :refer [list-dropdown]]))

(def operators [{:op         "LOOKUP"
                 :label      "Lookup"
                 :applies-to [nil]}
                {:op         "IN"
                 :label      "In some list"
                 :applies-to [nil]}
                {:op         "NOT IN"
                 :label      "Not in some list"
                 :applies-to [nil]}
                {:op         "="
                 :label      "="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         "!="
                 :label      "!="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         "CONTAINS"
                 :label      "Contains"
                 :applies-to ["java.lang.String"]}
                {:op         "<"
                 :label      "<"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         "<="
                 :label      "<="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         ">"
                 :label      ">"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         ">="
                 :label      ">="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op         "LIKE"
                 :label      "Like"
                 :applies-to ["java.lang.String"]}
                {:op         "NOT LIKE"
                 :label      "Not like"
                 :applies-to ["java.lang.String"]}
                {:op         "ONE OF"
                 :label      "One of"
                 :applies-to []}
                {:op         "NONE OF"
                 :label      "None of"
                 :applies-to []}])

(defn applies-to? [type op]
  (some true? (map (partial = type) (:applies-to op))))

(defn constraint-label [op]
  (:label (first (filter #(= op (:op %)) operators))))

(defn constraint-text-input []
  (fn [& {:keys [value on-change on-blur possible-values]}]
    [:div.dropdown
     [:input.form-control {:data-toggle "dropdown"
                           :type        "text"
                           :on-change   (fn [e] (on-change (oget e :target :value)))
                           :on-blur     (fn [x]
                                          (.log js/console (oget x :relatedTarget))
                                          (on-blur))
                           :value       value}]
     (when (not-empty possible-values)
       (into [:ul.dropdown-menu]
             (map (fn [v]
                    [:li {:on-click (fn [] (on-change v))}
                     [:a v]])) possible-values))]))




(defn constraint-operator []
  "Creates a dropdown for a query constraint.
  :model      The intermine model to use
  :path       The path of the constraint
  :op         The operator of the constraint
  :on-change  A function to call with the new operator
  :lists      (Optional) if provided, automatically disable list constraints
              if there are no lists of that type"
  (fn [& {:keys [model path op on-change lists]}]
    [:div.input-group-btn.dropdown
     [:button.btn.btn-default.dropdown-toggle
      ;:button.btn.btn-default.btn-raised.dropdown-toggle
      {:style       {:text-transform "none"}
       :data-toggle "dropdown"}
      (str (constraint-label op) " ") [:span.caret]]
     (let [path-class            (im-path/class model path)
           any-lists-with-class? (some? (some (fn [list] (= path-class (keyword (:type list)))) lists))]
       (into [:ul.dropdown-menu]
             (->> (filter (partial applies-to? (im-path/data-type model path)) operators)
                  (map (fn [{:keys [op label] :as operator}]
                         ; If we were given a collection of lists, and this operator is a list operator,
                         ; and there are no lists of the Class of this path, then disabled the operator.
                         (let [disabled? (if (and lists (not any-lists-with-class?) (or (= op "IN") (= op "NOT IN"))) "disabled")]
                           [:li
                            {:class    (if disabled? "disabled")
                             :on-click (if-not disabled? (partial on-change op))}
                            [:a label]]))))))]))

(defn constraint []
  "Creates a button group that represents a query constraint.
  :model      The Intermine model to use
  :path       The path of the constraint
  :value      The value of the constraint
  :code       The letter code of the constraint
  :op   The operator of the constraint
  :on-change  A function to call with the new constraint
  :on-remove A function to call with the constraint is removed
  :label?     If true then include the path as a label"
  (fn [& {:keys [lists model path value op code on-change on-remove on-blur label? possible-values]}]
    [:div.constraint-component
     [:div
      {:style {:display "table"}}

      [:div.input-group
       [constraint-operator
        :model model
        :path path
        :op op
        :lists lists
        :on-change (fn [op] (on-change {:code code :path path :value value :op op}))]
       (cond
         ; If this is a LIST constraint then show a list dropdown
         (or (= op "IN")
             (= op "NOT IN")) [list-dropdown
                               :value value
                               :lists lists
                               :restrict-type (im-path/class model path)
                               :on-change (fn [list] (on-change {:path path :value list :code code :op op}))]
         ; Otherwise show a text input
         :else [constraint-text-input
                :model model
                :value value
                :path path
                :possible-values possible-values
                :on-change (fn [val] (on-change {:path path :value val :op op :code code}))
                :on-blur on-blur])
       (when code [:span.constraint-label code])]
      [:i.fa.fa-trash-o.fa-fw.semilight.danger
       {:on-click (fn [op] (on-remove {:path path :value value :op op}))
        :style    {:display "table-cell" :vertical-align "middle"}}]]]))








