(ns re-frame-boiler.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [accountant.core :as accountant]
            [secretary.core :as secretary]
            [re-frame-boiler.components.lists.views :as list-views]
            [re-frame-boiler.components.templates.helpers :as helpers]))

(defn categories []
  (let [categories        (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills [:li {:on-click #(dispatch [:select-template-category nil])
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

(defn main []
  (let [im-templates      (subscribe [:templates-by-category])
        selected-template (subscribe [:selected-template])]
    (fn []
      [:div
       [:h2 "Popular Queries"]
       [:div
        [:div.row
         [:div.col-md-12 [categories]]]
        [:div.row
         [:div.col-md-6.fix-height-400-IGNORE [templates @im-templates]]
         [:div.col-md-6 [form @selected-template]]]]])))
