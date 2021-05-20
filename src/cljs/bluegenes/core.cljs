(ns bluegenes.core
  (:require [reagent.dom :as dom]
            [re-frame.core :as re-frame]
            [bluegenes.utils]
            [im-tables.core]
            [bluegenes.events]
            [bluegenes.subs]
            [bluegenes.route]
            [bluegenes.views :as views]
            [bluegenes.config]
            [bluegenes.pages.templates.core]
            [bluegenes.specs]
            [cljsjs.google-analytics]
            [cljsjs.react-transition-group]
            [cljsjs.react-day-picker]
            [cljsjs.react-select]
            [oops.core :refer [ocall]]))

;(defn dev-setup []
;  (when config/debug?
;    (devtools/install!)
;    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (dom/render [views/main-panel]
              (ocall js/document "getElementById" "app"))
  ;; Development mode helper code snippet
  ;; Uncomment this code to keep modals open after figwheel reloads.
  #_(-> (js/$ "#myModalsID") ; Replace with your modal id.
        (.addClass "in")
        (.css "display" "block")))

(defn ^:export init []
  (re-frame/dispatch-sync [:boot])
  ;(dev-setup)
  ; Initialize our bootstrap dropdowns
  (ocall (js/$ ".dropdown-toggle") :dropdown)
  (mount-root))
