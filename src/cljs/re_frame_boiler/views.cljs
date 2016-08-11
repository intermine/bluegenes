(ns re-frame-boiler.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [json-html.core :as json-html]
            [re-frame-boiler.components.nav :as nav]
            [re-frame-boiler.sections.home.views :as home]
            [re-frame-boiler.sections.assets.views :as assets]
            [imjs.user :as imjs]
            [accountant.core :refer [navigate!]]))

(defn debug-panel []
  (let [app-db (re-frame/subscribe [:app-db])]
    (fn []
      [:div
       [:button.btn {:on-click #(navigate! "#/assets/lists/123")} "Route"]
       [:div.panel.container
        [:div.title "Global Progress Bar"]
        [:button.btn
         {:on-click #(dispatch [:test-progress-bar (rand-int 101)])} "Random"]
        [:button.btn
         {:on-click #(dispatch [:test-progress-bar 0])} "Hide"]]
       (json-html/edn->hiccup (dissoc @app-db :assets))])))


;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a.callout {:on-click #(navigate! "#/")} "go to Home Page"] ]]))

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
      [:div.approot
       [nav/main]
       [show-panel @active-panel]])))
