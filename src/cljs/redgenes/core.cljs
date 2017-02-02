(ns redgenes.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [devtools.core :as devtools]
            [re-frisk.core :refer [enable-re-frisk!]]
    ;[redgenes.modules :as modules]
            [redgenes.utils]
            [im-tables.core]
            [redgenes.events]
            [redgenes.subs]
            [redgenes.routes :as routes]
            [redgenes.views :as views]
            [redgenes.config :as config]
    ;[redgenes.workers :as workers]
            [redgenes.components.querybuilder.subs]
            [redgenes.components.templates.core]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

(defn dev-setup []
  (when config/debug?
    (devtools/install!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (ocall js/document "getElementById" "app")))

(defn navigate-to-deep-links []
  (let [url (oget js/window "location" "hash")
        hashless-path (last (clojure.string/split url #"#/"))]
    (cond (> (count url) 2) ;; if there is more than #/ in the url, navigate there
      (navigate! hashless-path)
)))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:boot])
  (navigate-to-deep-links)
  (dev-setup)
  (mount-root))

;(modules/set-loaded! "app")
