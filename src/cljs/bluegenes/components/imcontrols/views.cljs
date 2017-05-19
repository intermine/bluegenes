(ns bluegenes.components.imcontrols.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget]]))



"Creates a dropdown of known organisms. The supplied :on-change function will
receive all attributes of the organism selected.
Options {}:
  :selected-value (optional) Supply this if you want to change the dropdown's selected-value
  :on-change Function to call when changed
Example usage:
  [im-controls/organism-dropdown
   {:selected-value     (if-let [sn (get-in @app-db [:my-tool :selected-organism :shortName])]
                 sn \"All Organisms\")
    :on-change (fn [organism]
                 (dispatch [:mytool/set-selected-organism organism]))}]
"
(defn organism-dropdown []
  (let [organisms (subscribe [:cache/organisms])]
    (fn [{:keys [selected-value on-change ]}]
      [:div.btn-group.organism-dropdown
       [:button.btn.btn-primary.dropdown-toggle
        {:data-toggle "dropdown"}
        [:span (if selected-value (str selected-value " ") "All Organisms ") [:span.caret]]]
       (-> [:ul.dropdown-menu]
           (into [[:li [:a {:on-click (partial on-change nil)}
                        [:span [:i.fa.fa-times] " Clear"]]]
                  [:li.divider]])
           (into (map (fn [organism]
                        [:li [:a
                              {:on-click (partial on-change organism)}
                              (:shortName organism)]])
                      (sort-by :shortName @organisms))))])))


(defn object-type-dropdown []
  (let [display-names @(subscribe [:model])]
  (fn [{:keys [values selected-value on-change ]}]
    [:div.btn-group.object-type-dropdown
     [:button.btn.btn-primary.dropdown-toggle
      {:data-toggle "dropdown"}
      [:span (if selected-value (str (get-in display-names [selected-value :displayName]) " ") "Select a type") [:span.caret]]]
     (-> [:ul.dropdown-menu]
         (into (map (fn [value]
            [:li [:a
                  {:on-click (partial on-change value)}
                  (get-in display-names [value :displayName])]])
          values)))])))


(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:title details)]
      (re-find (re-pattern (str "(?i)" string)) (clojure.string/join " " (map details [:name :description])))
      false)
    true))

(defn has-type?
  "Return true if a label contains a string"
  [type list]
  (if type
    (= type (keyword (:type list)))
    true))

(defn list-dropdown []
  (let [lists             (subscribe [:lists])
        filter-text       (reagent/atom nil)
        current-mine-name (subscribe [:current-mine-name])]
    (fn [{:keys [on-change value restricted-type]}]
      (let [text-filter (partial has-text? @filter-text)
            type-filter (partial has-type? restricted-type)
            lists (filter (apply every-pred [text-filter type-filter]) (@current-mine-name @lists))]
        [:div.dropdown
         [:span.dropdown-toggle
          {:style       {:color          "#039be5"
                         :text-transform "none"}
           :data-toggle "dropdown"}
          [:span (if (and (not= value "") (not= value nil)) value "Choose List ") [:span.caret]]]
         [:div.dropdown-menu.dropdown-mixed-content
          [:div.container-fluid
           [:form.form
            [:input.form-control
             {:type        "text"
              :value       @filter-text
              :on-change   (fn [e] (reset! filter-text (oget e :target :value)))
              :placeholder "Filter..."}]]

           (if (empty? @filter-text)
             [:div.row
              [:div.col-sm-6
               [:h4 [:i.fa.fa-clock-o] " Recently Created"]
               (into [:ul] (map (fn [{:keys [name size]}]
                                  [:li
                                   {:on-click (partial on-change {:name name :source @current-mine-name})}
                                   [:a
                                    [:span name]
                                    [:span.size (str " (" size ")")]]])
                                (take 5 (sort-by :timestamp lists))))

               [:div.sep {:style {:border-bottom "1px solid #dedede"}}]

               [:h4 [:i.fa.fa-clock-o] " Recently Used"]
               (into [:ul] (map (fn [{:keys [name size]}]
                                  [:li
                                   {:on-click (partial on-change {:name name :source @current-mine-name})}
                                   [:a [:span name]
                                    [:span.size (str " (" size ")")]]])
                                (take 5 lists)))]
              [:div.col-sm-6

               [:h4 [:i.fa.fa-sort-alpha-asc] " All Lists"]
               (into [:ul.clip-400] (map (fn [{:keys [name size]}]
                                           [:li
                                            {:on-click (partial on-change {:name name :source @current-mine-name})}
                                            [:a
                                             [:span name]
                                             [:span.size (str " (" size ")")]]])
                                         (sort-by :name lists)))]]
             [:div.col-sm-12
              [:h4 "Filtered..."]
              (into [:ul] (map (fn [{:keys [name size]}]
                                 [:li
                                  {:on-click (partial on-change {:name name :source @current-mine-name})}
                                  [:a
                                   [:span name]
                                   [:span.size (str " (" size ")")]]])
                               (sort-by :name lists)))])]]]))))


(def ops [{:op         "IN"
           :label      "In some list"
           :applies-to [:class]}
          {:op         "NOT IN"
           :label      "Not in some list"
           :applies-to [:class]}
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
           :applies-to ["java.lang.String" :class]}])


(defn applies-to? [type op] (some? (some #{type} (:applies-to op))))

(defn op-dropdown
  []
  (fn [constraint options]
    (let [{:keys [type on-change is-class? field-type]} options]
      [:div.dropdown
       [:button.btn.btn-default.btn-raised.dropdown-toggle
        {:style       {:text-transform "none"}
         :data-toggle "dropdown"}
        (str (or
               (:label (first (filter #(= (:op constraint) (:op %)) ops)))
               "Select")
             " ") [:span.caret]]
       (let [filtered-ops (filter (partial applies-to? type) ops)
             list-ops     (if is-class? (filter (partial applies-to? :class) ops))]
         (into [:ul.dropdown-menu]
               (concat
                 (if list-ops
                   (->> list-ops
                        (map (fn [o]
                               [:li
                                {:on-click (partial on-change (:op o))}
                                [:a (or (:label o) (:op o))]]))))
                 (if (and (not-empty filtered-ops) (not-empty list-ops)) [[:li.divider]])
                 (map (fn [o] [:li {:on-click (partial on-change (:op o))} [:a (or (:label o) (:op o))]])
                      filtered-ops))))])))
