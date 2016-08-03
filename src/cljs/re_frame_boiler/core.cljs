(ns re-frame-boiler.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [devtools.core :as devtools]
              [re-frame-boiler.handlers]
              [re-frame-boiler.subs]
              [re-frame-boiler.routes :as routes]
              [re-frame-boiler.views :as views]
              [re-frame-boiler.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")
    (devtools/install!)))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:fetch-all-assets])
  (dev-setup)
  (mount-root))
