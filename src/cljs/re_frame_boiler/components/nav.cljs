(ns re-frame-boiler.components.nav
  (:require [re-frame.core :as re-frame :refer [subscribe]]))

(defn main []
  (let [active-panel (subscribe [:active-panel])
        panel-is     (fn [panel-key] (= @active-panel panel-key))]
    (fn []
      [:nav.navbar.navbar-default
       [:div.container-fluid
        [:div.navbar-header
         [:a.navbar-brand {:href "#"} "APP"]
         [:ul.nav.navbar-nav
          [:li {:class (if (panel-is :about-panel) "active")} [:a {:href "#/about"} "About"]]
          [:li {:class (if (panel-is :debug-panel) "active")} [:a {:href "#/debug"} "Debug"]]]]]])))