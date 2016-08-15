(ns re-frame-boiler.components.lists.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [secretary.core :as secretary]
            [re-frame-boiler.components.templates.helpers :as helpers]))

(defn categories []
  (fn [categories]
    (into [:ul.nav.nav-pills [:li.active [:a "All"]]]
          (map (fn [category] [:li [:a category]]) categories))))


(defn im-list []
  (fn [l]
    [:a.list-group-item
     {:on-click (fn [] (dispatch [:select-template nil]))}
     [:h4.list-group-item-heading (str (:title l) " (" (:size l) " " (:type l) "s)")]
     ;[:p.list-group-item-text (str l)]
     ]))

(defn lists []
  (fn [lists]
    (into [:div] (map (fn [l] [im-list l]) lists))))

(defn main []
  (let [im-lists          (subscribe [:lists])
        selected-template (subscribe [:selected-template])]
    (fn []
      [:div.panel
       [:h2 "Lists"]
       [:div.container
        [:div.row
         [:div.col-md-12.fix-height-400 [lists @im-lists]]]]])))


(defn list-dropdown []
  (let [lists (subscribe [:lists])]
    (fn []
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        "Or Choose a List"]
       (into [:ul.dropdown-menu] (map (fn [l]
                                        [:li [:a (str (:name l))]]) @lists))])))