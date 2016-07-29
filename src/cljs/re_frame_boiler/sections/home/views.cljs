(ns re-frame-boiler.sections.home.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [re-frame-boiler.components.search :as search]))



(defn templates []
  (fn []
    [:div.panel
     [:h2 "Templates"]
     [:ul.list-group
      [:li.list-group-item "TEST"]
      [:li.list-group-item "TEST"]
      [:li.list-group-item "TEST"]]]))



(defn main []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.container
       [:div.row [:h3 (str "Welcome to " @name)]]
       [:div.row [search/main]]
       [:hr]
       [:div.row [templates]]
       [:hr]
       [:div.row [:span "test"]]])))