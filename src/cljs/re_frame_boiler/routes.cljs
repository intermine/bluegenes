(ns re-frame-boiler.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [accountant.core :as accountant]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [re-frame.core :as re-frame]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (accountant/navigate! (str "#" (.-token event)))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
            (re-frame/dispatch [:set-active-panel :home-panel]))

  (defroute "/about" []
            (re-frame/dispatch [:set-active-panel :about-panel]))

  (defroute "/debug" []
            (re-frame/dispatch [:set-active-panel :debug-panel]))

  (defroute "/list" []
            (re-frame/dispatch [:set-active-panel :list-panel]))

  (defroute "/templates" []
            (re-frame/dispatch [:set-active-panel :templates-panel]))

  (defroute "/querybuilder" []
            (re-frame/dispatch [:set-active-panel :querybuilder-panel
                                nil
                                [:qb-make-tree]]))

  (defroute "/assets/:type/:id" [type id]
            (re-frame/dispatch [:set-active-panel :list-panel {:type type :id id}]))

  (defroute "/objects/:type/:id" [type id]
            (re-frame/dispatch [:set-active-panel :object-panel
                                {:type type :id id}
                                [:load-report type id]]))

  ;; --------------------

  (accountant/configure-navigation!
    {:nav-handler  (fn [path] (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))})

  (hook-browser-navigation!))
