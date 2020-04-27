(ns bluegenes.components.ui.constraint
  (:require [imcljs.path :as im-path]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget ocall]]
            [clojure.string :refer [includes? blank? split join]]
            [bluegenes.components.loader :refer [mini-loader]]
            [reagent.core :as reagent :refer [create-class]]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [bluegenes.components.ui.list_dropdown :refer [list-dropdown]]
            [cljs-time.coerce :as time-coerce]
            [cljs-time.format :as time-format]))

; These are what appear in a constraint drop down in order. Each map contains:
;  :op         - The syntax found in InterMine Query Language
;  :label      - A more friendly version to display to users
;  :applies-to - The Java type that represents the field in InterMine
(def operators [{:op "LOOKUP"
                 :label "Lookup"
                 :applies-to [nil]}
                {:op "IN"
                 :label "In list"
                 :applies-to [nil]}
                {:op "NOT IN"
                 :label "Not in list"
                 :applies-to [nil]}
                {:op "="
                 :label "="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
                {:op "!="
                 :label "!="
                 :applies-to ["java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
                {:op "CONTAINS"
                 :label "Contains"
                 :applies-to ["java.lang.String"]}
                {:op "<"
                 :label "<"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
                {:op "<="
                 :label "<="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
                {:op ">"
                 :label ">"
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
                {:op ">="
                 :label ">="
                 :applies-to ["java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"]}
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

(def fmt "yyyy-MM-dd")
(def date-fmt (time-format/formatter fmt))

(defn read-day-change
  "Convert DayPicker input to the string we use as constraint."
  [date _mods picker]
  (let [input (-> (ocall picker :getInput)
                  (oget :value))]
    ;; `date` can be nil if it's not a valid date. We use the raw input text in
    ;; that case, to accomodate alien calendars.
    (or (some->> date
                 (time-coerce/from-date)
                 (time-format/unparse date-fmt))
        input)))

(defn date-constraint-input
  "Wraps `cljsjs/react-day-picker` for use as constraint input for selecting dates."
  [{:keys [value on-change on-blur]}]
  [:> js/DayPicker.Input
   {:inputProps {:class "form-control"}
    :value (or value "")
    :placeholder "YYYY-MM-DD"
    :formatDate (fn [date _ _]
                  (if (instance? js/Date date)
                    (->> date (time-coerce/from-date) (time-format/unparse date-fmt))
                    ""))
    :parseDate (fn [s _ _]
                 (when (and (string? s)
                            (= (count s) (count fmt)))
                   ;; Invalid dates like "2020-03-33" causes cljs-time
                   ;; to throw an error. We don't care and return nil.
                   (try
                     (some->> s (time-format/parse date-fmt) (time-coerce/to-date))
                     (catch js/Error _
                       nil))))
    :onDayChange (comp on-change read-day-change)
    :onDayPickerHide #(on-blur value)}])

(defn select-constraint-input
  "Wraps `cljsjs/react-select` for use as constraint input for selecting
  one value out of `possible-values`."
  [{:keys [model path value on-blur possible-values disabled]}]
  [:> js/Select
   {:className "constraint-select"
    :classNamePrefix "constraint-select"
    :placeholder (str "Choose "
                      (join " > " (take-last 2 (split (im-path/friendly model path) " > "))))
    :isDisabled disabled
    ;; Leaving the line below as it can be useful in the future.
    ; :isLoading (seq? possible-values)
    :onChange (fn [value]
                (on-blur (oget value :value)))
    :value (when (not-empty value) {:value value :label value})
    :options (map (fn [v] {:value v :label v}) (remove nil? possible-values))}])

(defn select-multiple-constraint-input
  "Wraps `cljsjs/react-select` for use as constraint input for selecting
  multiple values out of `possible-values`."
  [{:keys [model path value on-blur possible-values disabled]}]
  [:> js/Select
   {:className "constraint-select"
    :classNamePrefix "constraint-select"
    :placeholder (str "Choose "
                      (join " > " (take-last 2 (split (im-path/friendly model path) " > "))))
    :isMulti true
    :isDisabled disabled
    ;; Leaving the line below as it can be useful in the future.
    ; :isLoading (seq? possible-values)
    :onChange (fn [values]
                (on-blur (not-empty (map :value (js->clj values :keywordize-keys true)))))
    :value (map (fn [v] {:value v :label v}) value)
    :options (map (fn [v] {:value v :label v}) (remove nil? possible-values))}])

(defn list-constraint-input
  "Wraps `list-dropdown` for use as constraint input for IN and NOT IN list."
  [{:keys [model path value on-blur lists disabled]}]
  [list-dropdown
   :value value
   :lists lists
   :disabled disabled
   :restrict-type (im-path/class model path)
   :on-change (fn [list]
                (on-blur list))])

(defn text-constraint-input
  "Freeform textbox for String / Lookup constraints."
  []
  (let [focused? (reagent/atom false)]
    (fn [{:keys [value on-change on-blur disabled]}]
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
                               (reset! focused? false)))}])))

(defn constraint-input
  "Returns the appropriate input component for the constraint operator."
  [& {:keys [model path value typeahead? on-change on-blur _code lists type
             _allow-possible-values possible-values disabled op on-select-list]
      :as props}]
  (cond
    (= type "java.util.Date")
    [date-constraint-input props]

    (and (not= type "java.lang.Integer")
         typeahead?
         (seq? possible-values)
         (or (= op "=")
             (= op "!=")))
    [select-constraint-input props]

    (and (and typeahead? (seq? possible-values))
         (or (= op "ONE OF")
             (= op "NONE OF")))
    [select-multiple-constraint-input props]

    (or (= op "IN")
        (= op "NOT IN"))
    [list-constraint-input props]

    :else
    [text-constraint-input props]))

(defn one-of? [value col] (some? (some #{value} col)))
(def not-one-of? (complement one-of?))

(defn constraint-operator []
  "Creates a dropdown for a query constraint.
  :model      The intermine model to use
  :path       The path of the constraint
  :op         The operator of the constraint
  :on-change  A function to call with the new operator
  :lists      (Optional) if provided, automatically disable list constraints
              if there are no lists of that type"
  (fn [& {:keys [model path op on-change on-blur lists disabled value]}]
    (let [path-class (im-path/class model path)
          any-lists-with-class? (some? (some (fn [list] (= path-class (keyword (:type list)))) lists))
          disable-lists? (and lists (not any-lists-with-class?))]
      (into [:select.form-control.constraint-chooser
             {:disabled disabled
              :class (when disabled "disabled")
              :value op
              :on-change (fn [e]
                           (let [new-op (oget e :target :value)]
                             (if (and (one-of? new-op ["IN" "NOT IN"]) (not-one-of? op ["IN" "NOT IN"]))
                               (on-change (oget e :target :value))
                               ; Only fire the on-blur event when the operator has not changed from
                               ; a non-list operator to a list-operator.
                               ; Switching from "= Protein Domain" to "IN Protein Domain" doesn't make sense!
                               (on-blur (oget e :target :value)))))}]
            (as-> operators $
              (filter (partial applies-to? (im-path/data-type model path)) $)
              #_(cond->> $
                  disable-lists? (remove (comp #{"IN" "NOT IN"} :op)))
              (map (fn [{:keys [op label] :as operator}]
                         ; If we were given a collection of lists, and this operator is a list operator,
                         ; and there are no lists of the Class of this path, then disabled the operator.
                     [:option
                      {:value op}
                      label]) $)
              (cond-> $
                (and disable-lists? (nil? (im-path/data-type model path)))
                (concat [[:li
                          {:class "disabled"}
                          [:a (str "(No " (:displayName (first (im-path/walk model (name path-class)))) " lists)")]]])))))))

(defn single->multi [v]
  (cond-> v (string? v) list))

(defn multi->single [v]
  (cond-> v (seq? v) first))

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
                                     disabled label] :as con}]
                        (let [class? (im-path/class? model path)
                              op (or op (if class? "LOOKUP" "="))]
                          [:div.constraint-container
                           [:div.row.no-gutter
                            [:div.col-sm-12 [:label label]]]
                           [:div.row.no-gutter
                            [:div.col-sm-4.constraint-selector
                             (when (and code (not hide-code?))
                               [:span.constraint-label code])
                             [constraint-operator
                              :model model
                              :path path
                              :disabled disabled
                               ; Default to an OP if one has not been given
                              :op op
                              :value value
                              :lists lists
                              :on-blur (fn [op]
                                         (if (or (= op "ONE OF") (= op "NONE OF"))
                                           ((or on-blur on-change) {:code code :path path :values (single->multi value) :op op})
                                           ((or on-blur on-change) {:code code :path path :value (multi->single value) :op op})))
                              :on-change (fn [op]
                                           (if (or (= op "ONE OF") (= op "NONE OF"))
                                             ((or on-change-operator on-change) {:code code :path path :values (single->multi value) :op op})
                                             ((or on-change-operator on-change) {:code code :path path :value (multi->single value) :op op})))]]
                            [:div.col-sm-8.constraint-input
                             [constraint-input
                              :model model
                              :value value
                              :op op
                              :on-select-list on-select-list
                              :typeahead? typeahead?
                              :path path
                              :code code
                              :lists lists
                              :disabled disabled
                              :type (im-path/data-type model path)
                              :allow-possible-values (and (not= op "IN") (not= op "NOT IN"))
                              :possible-values @pv
                              :on-change (fn [val]
                                           (if (and (some? val) (or (= op "ONE OF") (= op "NONE OF")))
                                             (on-change {:path path :values val :op op :code code})
                                             (on-change {:path path :value val :op op :code code})))
                              :on-blur (fn [val]
                                         (if (and (some? val) (or (= op "ONE OF") (= op "NONE OF")))
                                           ((or on-blur on-change) {:path path :values val :op op :code code})
                                           ((or on-blur on-change) {:path path :value val :op op :code code})))]
                             (when on-remove
                               [:svg.icon.icon-bin
                                {:on-click (fn [op] (on-remove {:path path :value value :op op}))}
                                [:use {:xlinkHref "#icon-bin"}]])]]]
                          #_[:div.constraint-component
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
                                              ((or on-change-operator on-change) {:code code :path path :values (single->multi value) :op op})
                                              ((or on-change-operator on-change) {:code code :path path :value (multi->single value) :op op})))]
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
                                 :else [constraint-input
                                        :model model
                                        :value value
                                        :op op
                                        :typeahead? typeahead?
                                        :path path
                                        :disabled disabled
                                        :type (im-path/data-type model path)
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
                             (when on-remove

                               [:svg.icon.icon-bin
                                {:on-click (fn [op] (on-remove {:path path :value value :op op}))}
                                [:use {:xlinkHref "#icon-bin"}]])]))})))
