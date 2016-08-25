(ns re-frame-boiler.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [re-frame-boiler.components.lists.views :as list-views]
            [re-frame-boiler.components.templates.helpers :as helpers]
            [json-html.core :as json-html]
            [clojure.string :refer [split join]]))


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


(defn applies-to? [type op]
  (some? (some #{type} (:applies-to op))))

(defn constraint [idx state]
  (let [state (reagent/atom state)]
    (fn [idx constraint]
      [:div
       ;(str @state)
       [:span (join " > " (take-last 2 (split (:path constraint) ".")))]
       [:div.input-group
        [:div.input-group-btn
         [:button.btn.btn-default.dropdown-toggle
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
                       [:a op]])) (map :op (filter (partial applies-to? (:field-type constraint)) ops)))]
        [:input.form-control
         {:type      "text"
          :value     (:value @state)
          :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
          :on-blur   (fn [] (dispatch [:template-chooser/replace-constraint idx @state]))}]
        (if (= :class (:field-type constraint)) [:div.input-group-btn
                                           (let [select-function (fn [name]
                                                                   (swap! state assoc :value name :op "IN")
                                                                   (dispatch [:template-chooser/replace-constraint idx @state]))]
                                             [list-dropdown select-function])])]])))

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
  (let [selected-template (subscribe [:selected-template])]
    (fn [[id query]]
      [:a.list-group-item
       {:class    (if (= (name id) (:name @selected-template)) "active")
        :on-click (fn [] (dispatch [:template-chooser/choose-template id]))}
       [:h4.list-group-item-heading
        (last (clojure.string/split (:title query) "-->"))]
       [:p.list-group-item-text (:description query)]])))

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

(defn main []
  (let [im-templates         (subscribe [:templates-by-category])
        selected-template    (subscribe [:selected-template])
        filter-state         (reagent/atom nil)
        result-count         (subscribe [:template-chooser/count])
        counting?            (subscribe [:template-chooser/counting?])
        selected-constraints (subscribe [:template-chooser/selected-template-constraints])]
    (fn []
      [:div.container-fluid
       [:h2 "Popular Queries"]
       [:div.row
        [:div.col-md-6
         [:div.panel.panel-default
          [:div.panel-heading
           [:div.row
            [:div.col-md-12
             [:form.form
              [:div.form-group
               [:label.control-label "Filter description"]
               [template-filter filter-state]]
              [:div.form-group
               [:label.control-label "Filter by category"]
               [categories]]]]]]
          [:div.panel-body.fix-height-400
           [templates @im-templates]]]]
        [:div.col-md-6
         [:div.panel.panel-default
          [:div.panel-heading "Constraints"]
          [:div.panel-body
           ^{:key (:name @selected-template)} [form @selected-constraints]
           #_(json-html/edn->hiccup @selected-template)]]
         [:div.panel.panel-default
          [:div.panel-heading]
          [:div.panel-body
           (if @counting?
             [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
             [:h2 (str @result-count "Results")])]]]]])))