(ns redgenes.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [clojure.string :refer [split join]]
            [json-html.core :as json-html]
            [redgenes.components.lighttable :as lighttable]))





(def ops [{:op         "="
           :applies-to [:string :boolean :integer :double :float]}
          {:op         "!="
           :applies-to [:string :boolean :integer :double :float]}
          {:op         "CONTAINS"
           :applies-to [:string]}
          {:op         "<"
           :applies-to [:integer :double :float]}
          {:op         "<="
           :applies-to [:integer :double :float]}
          {:op         ">"
           :applies-to [:integer :double :float]}
          {:op         ">="
           :applies-to [:integer :double :float]}
          {:op         "LIKE"
           :applies-to [:string]}
          {:op         "NOT LIKE"
           :applies-to [:string]}
          {:op         "ONE OF"
           :applies-to []}
          {:op         "NONE OF"
           :applies-to []}
          {:op         "LOOKUP"
           :applies-to [:class]}])

(defn list-dropdown []
  (let [lists (subscribe [:lists])]
    (fn [update-fn]
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        [:i.fa.fa-list.pad-right-5] "List"]
       (into [:ul.dropdown-menu.dropdown-menu-right]
             (map (fn [l]
                    [:li
                     {:on-click (fn [] (update-fn (:name l)))}
                     [:a (str (:name l))]]) @lists))])))

(defn list-saveddata []
  (let [lists (subscribe [:saved-data/filtered-items])]
    (fn [update-fn]
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        [:i.fa.fa-list.pad-right-5] "SD"]
       (into [:ul.dropdown-menu.dropdown-menu-right]
             (map (fn [l]
                    ;(.log js/console l)
                    [:li
                     {:on-click (fn [] (update-fn (:name l)))}
                     [:a (str (:sd/label l))]]) @lists))])))



(defn categories []
  (let [categories        (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills
             [:li {:on-click #(dispatch [:template-chooser/set-category-filter nil])
                   :class    (if (nil? @selected-category) "active")}
              [:a "All"]]]
            (map (fn [category]
                   [:li {:on-click #(dispatch [:template-chooser/set-category-filter category])
                         :class    (if (= category @selected-category) "active")}
                    [:a category]])
                 @categories)))))


(defn applies-to? [type op] (some? (some #{type} (:applies-to op))))

(defn constraint [idx state]
  (let [state (reagent/atom state)]
    (fn [idx constraint]
      [:div.form-group.row
       [:div.col-xs-3
        [:h4
         {:style {:text-align "right"}}
         [:span (join " > " (take-last 2 (split (:path constraint) ".")))]]]
       [:div.col-xs-3.stretch-buttons
        {:style {:text-align "right"}}

        [:div.dropdown
         [:button.btn.btn-default.btn-raised.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          (:op @state)
          [:i.fa.fa-caret-down.pad-left-5]]
         (into [:ul.dropdown-menu]
               (map (fn [op]
                      [:li
                       {:on-click (fn []
                                    (swap! state assoc :op op)
                                    (dispatch [:template-chooser/replace-constraint idx @state]))}
                       [:a op]]))
               (map :op ops)
               ;(map :op (filter (partial applies-to? (:field-type constraint)) ops))
               )]]
       [:div.col-xs-6
        [:input.form-control
         {:type      "text"
          :value     (:value @state)
          :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
          :on-blur   (fn [] (dispatch [:template-chooser/replace-constraint idx @state]))}]]])))


(defn constraint-vertical [idx state]
  (let [state (reagent/atom state)]
    (fn [idx constraint]
      [:div

       [:div.form-group.row
        [:div.col-xs-offset-3
         {:style {:padding 0
                  :padding-left "20px"}
          }
         [:span (join " > " (take-last 2 (split (:path constraint) ".")))]]]


       [:div.form-group.row

        [:div.col-xs-3.stretch-buttons
         {:style {:text-align "right"}}

         [:div.dropdown
          [:button.btn.btn-default.btn-raised.dropdown-toggle
           {:type        "button"
            :data-toggle "dropdown"}
           (:op @state)
           [:i.fa.fa-caret-down.pad-left-5]]
          (into [:ul.dropdown-menu]
                (map (fn [op]
                       [:li
                        {:on-click (fn []
                                     (swap! state assoc :op op)
                                     (dispatch [:template-chooser/replace-constraint idx @state]))}
                        [:a op]]))
                (map :op ops)
                ;(map :op (filter (partial applies-to? (:field-type constraint)) ops))
                )]]


        [:div.col-xs-9
         [:input.form-control
          {:type      "text"
           :value     (:value @state)
           :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
           :on-blur   (fn [] (dispatch [:template-chooser/replace-constraint idx @state]))}]]






        #_[:div.col-xs-3
           [:h4
            {:style {:text-align "right"}}
            [:span (join " > " (take-last 2 (split (:path constraint) ".")))]]]

        ]])))

(defn form []
  (fn [constraints]
    [:div
     (if constraints
       (into [:form.form]
             (map (fn [[idx con]]
                    [constraint idx con])
                  (keep-indexed (fn [idx con]
                                  (if (:editable con)
                                    [idx con])) constraints))))]))

(defn template []
  (let [selected-template (subscribe [:selected-template])
        service           (subscribe [:selected-template-service])]
    (fn [[id query]]
      [:div.grid-1
       [:div.col.ani.template
        {:on-click (fn []
                     (if (not= (name id) (:name @selected-template))
                       (dispatch [:template-chooser/choose-template id])))
         :class    (if (= (name id) (:name @selected-template)) "selected")}
        [:div.title [:h4 (:title query)]]
        [:div.description (:description query)]
        (if (= (name id) (:name @selected-template))
          [:div.body
           [:div.col-xs-6.border-right
            (into [:form.form]
                  (concat
                    (map (fn [[idx con]]
                           [constraint-vertical idx con])
                         (keep-indexed (fn [idx con]
                                         (if (:editable con)
                                           [idx con])) (:where query)))
                    [[:div.form-group.row
                      [:div.col-xs-offset-6
                       {:style {:text-align "right"}}
                       [:button.btn.btn-primary.btn-raised
                        {:type "button"
                         :on-click (fn [] (dispatch [:templates/send-off-query]))}
                        "View All Results"]]]]))]

           [:div.col-xs-6
            {:style {:overflow-x "hidden"}}
            [:span "Results Preview"]
            [lighttable/main {:query      @selected-template
                              :service    @service
                              :no-repeats true}]]])]])))

(defn templates []
  (fn [templates]
    (into [:div] (map (fn [t] [template t]) templates))))

(defn template-filter []
  (let [text-filer (subscribe [:template-chooser/text-filter])]
    (fn []
      [:input.form-control.input-lg
       {:type        "text"
        :value       @text-filer
        :placeholder "Filter text..."
        :on-change   (fn [e]
                       (dispatch [:template-chooser/set-text-filter (.. e -target -value)]))}])))

(defn add-commas [num]
  (clojure.string/replace
    (js/String. num)
    (re-pattern "(\\d)(?=(\\d{3})+$)") "$1,"))


(defn no-results-yet []
  [:div.panel-body
   [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
   "Try clicking on a template name in the other pane to preview its results"]
  )



(defn results [counting? result-count selected-template]
  [:div.pane.pane-default.results
   [:div.pane-heading "Results preview " (cond @result-count (str "(" @result-count " rows)"))]
   (if (and (nil? @result-count) (not @counting?))
     [no-results-yet]
     [:div.pane-body
      (if @counting?
        [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
        [:div
         [:button.btn.btn-primary.btn-raised
          {:on-click
           (fn []
             (dispatch
               [:save-data {:sd/type    :query
                            :sd/service :flymine
                            :sd/label   (last (split (:title @selected-template) "-->"))
                            :sd/value   (assoc @selected-template :title (last (split (:title @selected-template) "-->")))}]))} "Save"]

         [:button.btn.btn-primary.btn-raised
          {:on-click (fn []
                       (dispatch [:templates/send-off-query]))}
          "View All Results"]
         #_[lighttable/main {:query      @selected-template
                             :no-repeats true}]
         ])])]
  )


(defn main []
  (let [im-templates         (subscribe [:templates-by-category])
        selected-template    (subscribe [:selected-template])
        filter-state         (reagent/atom nil)
        result-count         (subscribe [:template-chooser/count])
        counting?            (subscribe [:template-chooser/counting?])
        selected-constraints (subscribe [:template-chooser/selected-template-constraints])]
    (fn []
      [:div.container
       ;(json-html/edn->hiccup @selected-template)
       [:div.row
        [:div.col-xs-12
         [:div.form-group.template-filter
          [:label.control-label "Filter by category"]
          [categories]]
         [:div.form-group.template-filter
          [:label.control-label "Filter by description"]
          [template-filter filter-state]]
         [:div
          [templates @im-templates]]]
        #_[:div.col-xs-6
           (cond (seq @selected-constraints) ;;show it if/when there's something to see
                 [:div.x-section.see-template-details
                  [:div.pane
                   [:div.pane-heading "Constraints"]
                   [:div.pane-body
                    ^{:key (:name @selected-template)} [form @selected-constraints]
                    #_(json-html/edn->hiccup @selected-template)]]
                  [results counting? result-count selected-template]
                  ])]]])))



(defn main-old []
  (let [im-templates         (subscribe [:templates-by-category])
        selected-template    (subscribe [:selected-template])
        filter-state         (reagent/atom nil)
        result-count         (subscribe [:template-chooser/count])
        counting?            (subscribe [:template-chooser/counting?])
        selected-constraints (subscribe [:template-chooser/selected-template-constraints])]
    (fn []
      [:div.x-body.templates-section
       [:div.x-section.select-a-template
        [:div.x-container
         [:div.pane.x-container
          [:div.pane-heading "Popular Queries"]
          [:div.pane-body.x-container
           [:div.form-group.template-filter
            [:label.control-label "Filter by category"]
            [categories]]
           [:div.form-group.template-filter
            [:label.control-label "Filter by description"]
            [template-filter filter-state]]
           [:div.x-scrollable-content
            [templates @im-templates]]]]]]
       (cond (seq @selected-constraints) ;;show it if/when there's something to see
             [:div.x-section.see-template-details
              [:div.pane
               [:div.pane-heading "Constraints"]
               [:div.pane-body
                ^{:key (:name @selected-template)} [form @selected-constraints]
                #_(json-html/edn->hiccup @selected-template)]]
              ;[results counting? result-count selected-template]
              ])

       ])))
