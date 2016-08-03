(ns re-frame-boiler.views
  (:require [re-frame.core :as re-frame]
            [json-html.core :as json-html]
            [re-frame-boiler.components.nav :as nav]
            [re-frame-boiler.sections.home.views :as home]
            [re-frame-boiler.sections.assets.views :as assets]
            [imjs.user :as imjs]))

(defn debug-panel []
  (let [app-db (re-frame/subscribe [:app-db])]
    (fn []
      [:div (json-html/edn->hiccup (dissoc @app-db :assets))])))


;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a.callout {:href "#/"} "go to Home Page"] ]]))

;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home/main])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :list-panel [] [assets/main])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div
       [nav/main]
       [:div
        [show-panel @active-panel]]])))
