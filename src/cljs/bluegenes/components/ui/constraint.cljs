(ns bluegenes.components.ui.constraint
  (:require [imcljs.path :as im-path]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget ocall]]
            [clojure.string :refer [includes? blank? split join]]
            [bluegenes.components.loader :refer [mini-loader]]
            [reagent.core :as reagent :refer [create-class]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.components.ui.list_dropdown :refer [list-dropdown]]))

; These are what appear in a constraint drop down in order. Each map contains:
;  :op         - The syntax found in InterMine Query Language
;  :label      - A more friendly version to display to users
;  :applies-to - The Java type that represents the field in InterMine
(def operators [{:op "LOOKUP"
                 :label "Lookup"
                 :applies-to [nil]}
                {:op "IN"
                 :label "In some list"
                 :applies-to [nil]}
                {:op "NOT IN"
                 :label "Not in some list"
                 :applies-to [nil]}
                {:op "="
                 :label "="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op "!="
                 :label "!="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op "CONTAINS"
                 :label "Contains"
                 :applies-to ["java.lang.String"]}
                {:op "<"
                 :label "<"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op "<="
                 :label "<="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op ">"
                 :label ">"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op ">="
                 :label ">="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float"]}
                {:op "LIKE"
                 :label "Like"
                 :applies-to ["java.lang.String"]}
                {:op "NOT LIKE"
                 :label "Not like"
                 :applies-to ["java.lang.String"]}
                {:op "ONE OF"
                 :label "One of"
                 :applies-to ["java.lang.String"]}
                {:op "NONE OF"
                 :label "None of"
                 :applies-to ["java.lang.String"]}])

(defn applies-to?
  "Given a field type (ex java.lang.Double) return all constraint maps that support that type"
  [type op]
  (some true? (map (partial = type) (:applies-to op))))

(defn constraint-label
  "Return the label for a given operator.
  This looks a little complicated because we want our constraints to have order
  so we need to filter the collection, get the first constraint map (they should all be unique anyway)
  then gets its label"
  [op]
  (:label (first (filter #(= op (:op %)) operators))))

(defn has-text?
  "Return true v contains a string"
  [string v]
  (if string
    (if v
      (re-find (re-pattern (str "(?i)" string)) v)
      false)
    true))

(defn has-three-matching-letters?
  "Return true if v contains a string and that string is 3 or more characters.
  (Silly but it speeds up text filtering)"
  [string v]
  (if (and string (>= (count string) 3))
    (if v
      (re-find (re-pattern (str "(?i)" string)) v)
      false)
    false))

; Make NodeList javascript objects seqable (can be used with map / reduce etc)
(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn set-text-value [node value]
  (when node (-> (sel1 node :input) (dommy/set-value! value))))


(defn constraint-text-input
  "A component that represents the freeform textbox for String / Lookup constraints"
  []
  (let [multiselects (reagent/atom {})
        focused? (reagent/atom false)]
    (fn [& {:keys [disabled model path value typeahead? on-change on-blur
                   allow-possible-values possible-values disabled op] :as x}]
      (if (and typeahead? (seq? possible-values))
        (cond
          (or
            (= op "=")
            (= op "!=")) [:div.constraint-text-input
                          {:class (when @focused? "open")}
                          (into [:select.form-control
                                 {:disabled disabled
                                  :class (when disabled "disabled")
                                  :value (or value "")
                                  :on-change (fn [e] (on-blur (oget e :target :value)))}]
                                (cond-> (map (fn [v] [:option {:value v} v]) (remove nil? possible-values))
                                        (blank? value) (conj
                                                         [:option {:disabled true :value ""}
                                                          (str
                                                            "Choose "
                                                            (join " > "
                                                                  (take-last 2 (split (im-path/friendly model path) " > "))))])))]
          (or
            (= op "ONE OF")
            (= op "NONE OF")) [:div.constraint-text-input
                               {:class (when @focused? "open")}
                               (into [:select.form-control
                                      {:multiple true
                                       :disabled disabled
                                       :class (when disabled "disabled")
                                       :value (or value [])
                                       :on-change (fn [e]
                                                    (let [value (doall (map first (filter (fn [[k elem]] (oget elem :selected)) @multiselects)))]
                                                      (on-blur value)))}]
                                     (map (fn [v]
                                            [:option
                                             {:ref (fn [e] (when e (swap! multiselects assoc v e)))
                                              :value v}
                                             v])
                                          (remove nil? possible-values)))]
          :else [:input.form-control
                 {:data-toggle "none"
                  :disabled disabled
                  :class (when disabled "disabled")
                  :type "text"
                  :value value
                  :on-focus (fn [e] (reset! focused? true))
                  :on-change (fn [e] (on-change (oget e :target :value)))
                  :on-blur (fn [e] (on-blur (oget e :target :value)) (reset! focused? false))
                  :on-key-down (fn [e] (when (= (oget e :keyCode) 13)
                                         (on-blur (oget e :target :value))
                                         (reset! focused? false)))}])
        [:input.form-control
         {:data-toggle "none"
          :disabled disabled
          :class (when disabled "disabled")
          :type "text"
          :value value
          :on-focus (fn [e] (reset! focused? true))
          :on-change (fn [e] (on-change (oget e :target :value)))
          :on-blur (fn [e] (on-blur (oget e :target :value)) (reset! focused? false))
          :on-key-down (fn [e] (when (= (oget e :keyCode) 13)
                                 (on-blur (oget e :target :value))
                                 (reset! focused? false)))}]))))


(defn constraint-operator []
  "Creates a dropdown for a query constraint.
  :model      The intermine model to use
  :path       The path of the constraint
  :op         The operator of the constraint
  :on-change  A function to call with the new operator
  :lists      (Optional) if provided, automatically disable list constraints
              if there are no lists of that type"
  (fn [& {:keys [model path op on-change lists disabled]}]
    [:div.input-group-btn.dropdown.constraint-operator
     [:button.btn.btn-default.dropdown-toggle
      {:data-toggle "dropdown" :disabled disabled}
      (str (constraint-label op) " ") [:span.caret {:style {:margin-left "5px"}}]]
     (let [path-class (im-path/class model path)
           any-lists-with-class? (some? (some (fn [list] (= path-class (keyword (:type list)))) lists))
           disable-lists? (and lists (not any-lists-with-class?))]
       (into [:ul.dropdown-menu]
             (as-> operators $
                   (filter (partial applies-to? (im-path/data-type model path)) $)
                   (cond->> $
                            disable-lists? (remove (comp #{"IN" "NOT IN"} :op)))
                   (map (fn [{:keys [op label] :as operator}]
                          ; If we were given a collection of lists, and this operator is a list operator,
                          ; and there are no lists of the Class of this path, then disabled the operator.
                          [:li
                           {:on-click (partial on-change op)}
                           [:a (case op
                                 "IN" (str "In a " (:displayName (first (im-path/walk model (name path-class)))) " list")
                                 "NOT IN" (str "Not in a " (:displayName (first (im-path/walk model (name path-class)))) " list")
                                 (str label))]]) $)
                   (cond-> $
                           (and disable-lists? (nil? (im-path/data-type model path))) (concat [[:li
                                                                                                {:class "disabled"}
                                                                                                [:a (str "(No " (:displayName (first (im-path/walk model (name path-class)))) " lists)")]]])))))]))

(defn constraint [& {:keys [model path]}]
  "Creates a button group that represents a query constraint.
  :model      The Intermine model to use
  :path       The path of the constraint
  :value      The value of the constraint
  :code       The letter code of the constraint
  :op   The operator of the constraint
  :on-change  A function to call with the new constraint
  :on-remove A function to call with the constraint is removed
  :label?     If true then include the path as a label
  :hide-code?     If true then do not show the code letter
  "
  (let [pv (subscribe [:current-possible-values path])]
    (create-class
      {:component-did-mount (fn []
                              (when (nil? @pv)
                                (dispatch [:cache/fetch-possible-values path])))
       :reagent-render (fn [& {:keys [lists model path value op code on-change
                                      on-select-list on-change-operator on-remove
                                      on-blur label? possible-values typeahead? hide-code?
                                      disabled label]}]
                         (let [class? (im-path/class? model path)
                               op (or op (if class? "LOOKUP" "="))]
                           [:div.constraint-component
                            [:div.input-group.constraint-input
                             [constraint-operator
                              :model model
                              :path path
                              :disabled disabled
                              ; Default to an OP if one has not been given
                              :op op
                              :lists lists
                              :on-change (fn [op]
                                           (if (or (= op "ONE OF") (= op "NONE OF"))
                                             ((or on-change-operator on-change) {:code code :path path :values (cond-> value string? list) :op op})
                                             ((or on-change-operator on-change) {:code code :path path :value (cond-> value (seq? value) first) :op op})))]
                             [:div
                              [:span.constraint-component-label label]
                              (cond
                                ; If this is a LIST constraint then show a list dropdown
                                (or (= op "IN")
                                    (= op "NOT IN")) [list-dropdown
                                                      :value value
                                                      :lists lists
                                                      :disabled disabled
                                                      :restrict-type (im-path/class model path)
                                                      :on-change (fn [list]
                                                                   ((or on-select-list on-change) {:path path :value list :code code :op op}))]
                                ; Otherwise show a text input
                                :else [constraint-text-input
                                       :model model
                                       :value value
                                       :op op
                                       :typeahead? typeahead?
                                       :path path
                                       :disabled disabled
                                       :allow-possible-values (and (not= op "IN") (not= op "NOT IN"))
                                       :possible-values @pv
                                       :on-change (fn [val]
                                                    (if (and (some? val) (or (= op "ONE OF") (= op "NONE OF")))
                                                      (on-change {:path path :values val :op op :code code})
                                                      (on-change {:path path :value val :op op :code code})))
                                       :on-blur (fn [val]
                                                  (if (and (some? val) (or (= op "ONE OF") (= op "NONE OF")))
                                                    ((or on-blur on-change) {:path path :values val :op op :code code})
                                                    ((or on-blur on-change) {:path path :value val :op op :code code})))])]
                             (when (and code (not hide-code?)) [:span.constraint-label code])]
                            (when on-remove [:svg.icon.icon-bin
                                             {:on-click (fn [op] (on-remove {:path path :value value :op op}))}
                                             [:use {:xlinkHref "#icon-bin"}]
                                             ])]))})))
