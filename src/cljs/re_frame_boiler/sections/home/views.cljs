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

(defn header []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.header
       [:h3 (str "Welcome to " @name)]
       [search/main]])))

(defn main []
  (fn []
    [:div
     [header]
     [:div.container
      [:div.row [templates]]
      [:hr]
      [:div.row [:span "test"]]]]))