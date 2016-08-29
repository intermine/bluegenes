(ns re-frame-boiler.components.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [re-frame-boiler.components.search :as search]
            [accountant.core :refer [navigate!]]
            [re-frame-boiler.components.progress_bar :as progress-bar]))

(defn settings []
  (fn []
    [:li.dropdown
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-cog]]
     [:ul.dropdown-menu
      [:li [:a {:on-click #(navigate! "#/debug")} [:i.fa.fa-terminal] " Developer"]]]]))

(defn logged-in [user]
  [:li.dropdown.active
   [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-user]]
   [:ul.dropdown-menu
    [:div.logged-in
     [:i.fa.fa-check-circle.fa-3x] (str (:username user))]
    [:li [:a {:on-click #(dispatch [:log-out])} "Log Out"]]]])

(defn anonymous []
  [:li.dropdown
   [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-user-times]]
   [:ul.dropdown-menu
    [:li [:a {:on-click #(dispatch [:log-in])} "Log In"]]]])

(defn user []
  (let [who-am-i (subscribe [:who-am-i])]
    (fn []
      (if @who-am-i
        [logged-in @who-am-i]
        [anonymous]))))

(defn main []
  (let [active-panel (subscribe [:active-panel])
        app-name     (subscribe [:name])
        saved-data   (subscribe [:saved-data])
        panel-is     (fn [panel-key] (= @active-panel panel-key))]
    (reagent/create-class
      {:component-did-update
       (fn [vals])
       :reagent-render
       (fn []
         [:nav.navbar.navbar-default.navbar-fixed-top.down-shadow
          [:div.container-fluid
           [:div.navbar-header
            [:span.navbar-brand {:on-click #(navigate! "#/")} @app-name]]
           [:ul.nav.navbar-nav.navbar-collapse.navigation
            [:li {:class (if (panel-is :home-panel) "active")} [:a {:on-click #(navigate! "#/")} "Home"]]
            [:li {:class (if (panel-is :upload-panel) "active")} [:a {:on-click #(navigate! "#/upload")} "Upload"]]
            [:li {:class (if (panel-is :templates-panel) "active")} [:a {:on-click #(navigate! "#/templates")} "Templates"]]
            [:li {:class (if (panel-is :querybuilder-panel) "active")} [:a {:on-click #(navigate! "#/querybuilder")} "Query Builder"]]
            [:li {:class (if (panel-is :saved-data) "active")} [:a {:on-click #(navigate! "#/saved-data")} (str "Saved Data (" (count (keys @saved-data)) ")")]]]
           [:ul.nav.navbar-nav.navbar-right.buttons
            [:li.search [search/main]]
            [:li [:a [:i.fa.fa-question]]]
            [user]
            [settings]]]
          [progress-bar/main]])})))
