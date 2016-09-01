(ns redgenes.sections.saveddata.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [redgenes.sections.saveddata.events]
            [reagent.core :as reagent]
            [redgenes.sections.saveddata.subs]
            [cljs-time.format :as tf]
            [json-html.core :as json-html]
            [accountant.core :refer [navigate!]]
            [inflections.core :refer [plural]]))


(def built-in-formatter (tf/formatter "HH:mm:ss dd/MM/YYYY"))

(defn toolbar []
  [:div.navbar.navbar-inverse
   [:div.container-fluid
    [:div.navbar-collapse.collapse.navbar-material-light-blue-collapse
     [:ul.nav.navbar-nav
      [:li.btn.btn-info.btn-raised
       {:on-click (fn [] (dispatch [:saved-data/toggle-edit-mode]))} "Merge Lists"]]]]])


(defn simple-breakdown []
  (let [model (subscribe [:model])]
    (fn [deconstructed-query]
      [:div.panel.panel-default
       (into [:div.panel-body]
             (map (fn [category-kw]
                    (let [display-name (plural (get-in @model [category-kw :displayName]))]
                      [:div.category display-name]))
                  (keys deconstructed-query)))])))


(defn saved-data-item []
  (let [edit-mode (subscribe [:saved-data/edit-mode])]
    (fn [[id {:keys [parts created label type value] :as all}]]
      [:div.col
       [:div.saved-data-item.panel.panel-default
        {:class (if @edit-mode "editing")}
        [:div.panel-heading
         [:div.save-bar
          [:span (tf/unparse built-in-formatter created)]
          [:i.fa.fa-2x.fa-times]
          [:i.fa.fa-2x.fa-star]
          ]]
        [:div.panel-body
         [:h3 (str label)]
         [simple-breakdown parts]
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn []
                       (dispatch ^:flush-dom [:results/set-query value])
                       (navigate! "#/results"))}
          "View"]]]])))

(defn debug []
  (let [saved-data-section (subscribe [:saved-data/section])]
    [:div
     (json-html/edn->hiccup @saved-data-section)]))

(defn main []
  (let [saved-data (subscribe [:saved-data/all])]
    (reagent/create-class
      {:component-did-mount
       (fn [e] (let [node (-> e reagent/dom-node js/$)]))
       :reagent-render
       (fn []
         [:div {:style {:margin-top "-10px"}}
          [toolbar]
          [:div.container-fluid
           [:h1 "Saved Data"]
           [:div.container
            [:span "Today"]
            (into [:div.grid-4_md-3_sm-1.saved-data-container]
                  (map (fn [e] [saved-data-item e]) @saved-data))]]
          ;[debug]
          ])})))