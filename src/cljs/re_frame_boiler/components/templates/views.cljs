(ns re-frame-boiler.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [accountant.core :as accountant]
            [secretary.core :as secretary]
            [re-frame-boiler.components.templates.helpers :as helpers]))

(defn categories []
  (fn [categories]
    (into [:ul.nav.nav-pills [:li.active [:a "All"]]]
          (map (fn [category] [:li [:a category]]) categories))))


(defn constraint []
  (fn [constraint]
    [:div.row
     [:span (str constraint)]
     [:label (:path constraint)]
     [:input.form-control {:type "text" :value (:value constraint)}]]))

(defn form []
  (fn [details]
    (if details (into [:div.form-group]
                      (map (fn [con] [constraint con]) (:where details))))))

(defn template []
  (fn [t]
    (let [[id query] t]
      [:a.list-group-item
       {:on-click (fn [] (dispatch [:select-template id]))}
       [:h4.list-group-item-heading
        (last (clojure.string/split (:title query) "-->"))]
       [:p.list-group-item-text (:description query)]])))

(defn templates []
  (fn [templates]
    (into [:div] (map (fn [t] [template t]) templates))))



(defn main []
  (let [im-templates      (subscribe [:templates])
        selected-template (subscribe [:selected-template])]
    (fn []
      [:div.panel
       [:div.btn {:on-click #(accountant/navigate! "assets/lists/123")} "NAV"]
       [:h2 "Popular Queries"]
       [:div.container
        [:div.row
         [:div.col-md-12 [categories (helpers/categories @im-templates)]]]
        [:div.row
         [:div.col-md-6.fix-height-400 [templates @im-templates]]
         [:div.col-md-6 [form @selected-template]]]]])))
