(ns re-frame-boiler.components.nav
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]))

(defn settings []
  (fn []
    [:li.dropdown
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-cog]]
     [:ul.dropdown-menu
      [:li [:a {:href "/#/debug"} [:i.fa.fa-terminal] " Developer"]]]]))

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
        panel-is     (fn [panel-key] (= @active-panel panel-key))]
    (fn []
      [:nav.navbar.navbar-default.navbar-fixed-top.down-shadow
       [:div.container-fluid
        [:div.navbar-header
         [:span.navbar-brand {:href "/#"} @app-name]]
        [:ul.nav.navbar-nav.navbar-collapse
         [:li {:class (if (panel-is :home-panel) "active")} [:a {:href "/#"} "Home"]]
         [:li {:class (if (panel-is :about-panel) "active")} [:a {:href "/#/about"} "About"]]]
        [:ul.nav.navbar-nav.navbar-right
         [:li [:a {:href "/#"} [:i.fa.fa-question]]]
         [user]
         [settings]]]])))