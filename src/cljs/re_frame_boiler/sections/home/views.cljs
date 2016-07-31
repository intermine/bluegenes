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
       [search/main]])))

(defn welcome []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.welcome
       [:h3 "Welcome to Intermine"]])))

(defn main []
  (fn []
    [:div
     ;[welcome]
     [header]
     [:div.container.padme
      [:div.row [templates]]]]))