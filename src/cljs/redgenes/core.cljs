(ns redgenes.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [devtools.core :as devtools]
    ;[redgenes.modules :as modules]
            [redgenes.utils]
            [redgenes.events]
            [redgenes.subs]
            [redgenes.routes :as routes]
            [redgenes.views :as views]
            [redgenes.config :as config]
    ;[redgenes.workers :as workers]
            [redgenes.components.listanalysis.core]
            [redgenes.components.querybuilder.subs]
            [redgenes.components.templates.core]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")
    (devtools/install!)))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (ocall js/document "getElementById" "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:fetch-all-assets])
  (dev-setup)
  (mount-root))

;(modules/set-loaded! "app")
