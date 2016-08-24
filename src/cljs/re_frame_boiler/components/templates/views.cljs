(ns re-frame-boiler.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [accountant.core :as accountant]
            [secretary.core :as secretary]
            [re-frame-boiler.components.lists.views :as list-views]
            [re-frame-boiler.components.templates.helpers :as helpers]
            [inflections.core :as inflections]))


(def ops
  ["=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF" "LOOKUP"])

(defn list-dropdown []
  (let [lists (subscribe [:lists])]
    (fn []
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        [:i.fa.fa-list.pad-right-5] "List"]
       (into [:ul.dropdown-menu] (map (fn [l]
                                        [:li [:a (str (:name l))]]) @lists))])))

(defn op []
  (let [state (reagent/atom {:op "="})]
    (fn [path]
      [:div
       [:span (str path)]
       [:span (str @state)]
       [:div.input-group
        [:div.input-group-btn
         [:button.btn.btn-default.dropdown-toggle
          {:type        "button"
           :data-toggle "dropdown"}
          (:op @state)
          [:i.fa.fa-caret-down.pad-left-5]]
         (into [:ul.dropdown-menu]
               (map (fn [op] [:li {:on-click (fn [] (swap! state assoc :op op))} [:a op]])) ops)]
        [:input.form-control
         {:type      "text"
          :value     (:value @state)
          :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))}]
        [:div.input-group-btn [list-dropdown]]]
       [:button.btn.btn-success
        {:on-click (fn [] (dispatch [:add-constraint (merge @state {:path path})]))} "Add"]])))


(defn categories []
  (let [categories        (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills
             [:li {:on-click #(dispatch [:select-template-category nil])
                   :class    (if (nil? @selected-category) "active")}
              [:a "All"]]]
            (map (fn [category] [:li {:on-click #(dispatch [:select-template-category category])
                                      :class    (if (= category @selected-category) "active")}
                                 [:a category]])
                 @categories)))))


(defn constraint []
  (fn [constraint]
    [:div.row
     ;[:span (str constraint)]
     [:label (:path constraint)]
     [:input.form-control {:type "text" :value (:value constraint)}]]))

(defn form []
  (fn [details]
    [:div
     ;(str details)
     (if details (into [:div.form-group]
                       (map (fn [con] [constraint con]) (:where details))))
     [list-views/list-dropdown]]))

(defn template []
  (let [selected-template-name (subscribe [:selected-template-name])]
    (fn [t]
      (let [[id query] t]
        [:a.list-group-item
         {:class    (if (= id @selected-template-name) "active")
          :on-click (fn [] (dispatch [:select-template id]))}
         [:h4.list-group-item-heading
          (last (clojure.string/split (:title query) "-->"))]
         [:p.list-group-item-text (:description query)]]))))

(defn templates []
  (fn [templates]
    (into [:div] (map (fn [t] [template t]) templates))))

(defn template-filter-fn [s [id details]]
  (if s
    (if-let [description (:description details)]
      (re-find (re-pattern (str "(?i)" s)) (:description details)))
    true))

(defn template-filter []
  (fn [filter-state]
    [:input.form-control.input-lg {:type        "text"
                                   :value       @filter-state
                                   :placeholder "Filter text..."
                                   :on-change   (fn [e] (reset! filter-state (.. e -target -value)))}]))

(defn main []
  (let [im-templates      (subscribe [:templates-by-category])
        selected-template (subscribe [:selected-template])
        filter-state      (reagent/atom nil)]
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
          [:div.panel-body
           [templates (filter (partial template-filter-fn @filter-state) @im-templates)]]]]
        [:div.col-md-6
         [:div.panel.panel-default
          [:div.panel-heading "Constraints"]
          [:div.panel-body [form @selected-template]]]]]])))
