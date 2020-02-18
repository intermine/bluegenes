(ns bluegenes.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [bluegenes.utils]
            [im-tables.core]
            [bluegenes.events]
            [bluegenes.subs]
            [bluegenes.route :as route]
            [bluegenes.views :as views]
            [bluegenes.config :as config]
            [bluegenes.pages.templates.core]
            [cljsjs.google-analytics]
            [cljsjs.react-transition-group]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]))

;(defn dev-setup []
;  (when config/debug?
;    (devtools/install!)
;    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (route/init-routes!)
  (reagent/render [views/main-panel]
                  (ocall js/document "getElementById" "app"))
  ;; Development mode helper code snippet
  ;; Uncomment this code to keep modals open after figwheel reloads.
  #_(-> (js/$ "#myMineOrganizeConfirm") ; Replace with your modal id.
        (.addClass "in")
        (.css "display" "block")))

(defn ^:export init []
  (re-frame/dispatch-sync [:boot])
  ;(dev-setup)
  ; Initialize our bootstrap dropdowns
  (ocall (js/$ ".dropdown-toggle") :dropdown)
  (mount-root))
