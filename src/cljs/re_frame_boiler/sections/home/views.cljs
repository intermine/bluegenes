(ns re-frame-boiler.sections.home.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [re-frame-boiler.components.search :as search]))



(defn generic-section []
  (fn []
    [:div.panel
     [:h2 "Some Component"]
     [:ul.list-group
      [:li.list-group-item "Data"]
      [:li.list-group-item "More Data"]
      [:li.list-group-item "And some more data"]]]))

(defn footer []
  (fn []
    [:footer.footer
     [:h1 "bottom"]]))

(defn header []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.header
       ;[search/main]
       ])))

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
      [:div.row [generic-section]]]]))