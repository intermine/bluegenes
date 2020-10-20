(ns bluegenes.pages.reportpage.components.sidebar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]))

(defn main []
  [:div.sidebar
   [:div.sidebar-entry
    [:h4 "Lists"]
    [:ul
     [:li [:a "PL FlyTF_PWM_TFs (129)"]]
     [:li [:a "PL FlyTF_putativeTFs (757)"]]]]
   [:div.sidebar-entry
    [:h4 "Other mines"]
    [:ul
     [:li [:a "Humanmine"]]
     [:li [:a "Flymine"]]]]
   [:div.sidebar-entry
    [:h4 "External resources"]
    [:ul
     [:li [:a "Humanmine"]]
     [:li [:a "Flymine"]]]]])
