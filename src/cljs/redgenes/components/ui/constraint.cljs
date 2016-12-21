(ns redgenes.components.ui.constraint
  (:require [imcljs.path :as im-path]
            [oops.core :refer [oget]]))

(def operators [{:op         "IN"
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
                 :applies-to []}
                {:op         "LOOKUP"
                 :label      "Lookup"
                 :applies-to [nil]}])

(defn applies-to? [type op]
  (some true? (map (partial = type) (:applies-to op))))

(defn label-for [op]
  (:label (first (filter #(= op (:op %)) operators))))

(defn constraint-input []
  (fn [& {:keys [model path value on-change]}]
    [:div
     [:label (im-path/friendly model path)]
     [:input.form-control {:type      "text"
                           :on-change (fn [e] (on-change (oget e :target :value)))
                           :value     value}]]))

(defn constraint-operator []
  "Creates a dropdown for a query constraint.
  :model      The intermine model to use
  :path       The path of the constraint
  :op         The operator of the constraint
  :on-change  A function to call with the new operator"
  (fn [& {:keys [model path op on-change]}]
    [:div.input-group-btn
     [:button.btn.btn-default.dropdown-toggle
      {:style       {:text-transform "none"}
       :data-toggle "dropdown"}
      (str (label-for op) " ") [:span.caret]]
     (into [:ul.dropdown-menu]
           (->> (filter (partial applies-to? (im-path/data-type model path)) operators)
                (map (fn [operator]
                       [:li
                        {:on-click (partial on-change (:op operator))}
                        [:a (:label operator)]]))))]))

(defn constraint []
  "Creates a button group that represents a query constraint.
  :model      The Intermine model to use
  :path       The path of the constraint
  :value      The value of the constraint
  :op   The operator of the constraint
  :on-change  A function to call with the new constraint
  :label?     If true then include the path as a label"
  (fn [& {:keys [model path value op on-change] :as args}]
    [:div.input-group
     [constraint-operator
      :model model
      :path path
      :op op
      :on-change (fn [op]
                   (on-change {:path  path
                               :value value
                               :op    op}))]
     [constraint-input
      :model model
      :value value
      :path path
      :on-change (fn [val]
                   (on-change {:path  path
                               :value val
                               :op    op}))]]))






