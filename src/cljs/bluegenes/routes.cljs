(ns bluegenes.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import goog.History)
  (:require [secretary.core :as secretary]
            [accountant.core :as accountant]
            [re-frame.core :as re-frame :refer [subscribe]]))


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

  (defroute "/help" []
            (re-frame/dispatch [:set-active-panel :help-panel]))

  (defroute "/templates" []
            (re-frame/dispatch [:set-active-panel :templates-panel]))

  (defroute "/upload" []
            (re-frame/dispatch [:set-active-panel :upload-panel]))

  (defroute "/explore" []
            (re-frame/dispatch [:set-active-panel :explore-panel]))

  (defroute "/search" []
            (re-frame/dispatch [:set-active-panel :search-panel]))

  (defroute "/querybuilder" []
            (re-frame/dispatch [:set-active-panel :querybuilder-panel
                                nil
                                [:qb/make-tree]]))

  (defroute "/results" []
            (re-frame/dispatch [:set-active-panel :results-panel]))

  (defroute "/regions" []
            (re-frame/dispatch [:set-active-panel :regions-panel]))

  ;(defroute "/saved-data" []
  ;          (re-frame/dispatch [:set-active-panel :saved-data-panel]))
  (defroute "/saved-data" []
            (re-frame/dispatch [:set-active-panel :saved-data-panel]))

  (defroute "/reportpage/:mine/:type/:id" [mine type id]
            (re-frame/dispatch [:set-active-panel :reportpage-panel
                                {:type type :id id :mine mine}
                                [:load-report mine type id]]))

  ;; --------------------

  (accountant/configure-navigation!
   ;;We use a custom brew accountant version which navigates based on fragments. this prevents the annoying double back button problem where the homepage kept on popping up in the history when we pressed the back button even though we hadn't been to the homepage at that point in the navigation flow.
    {:nav-handler  (fn [path] (secretary/dispatch! path))
     :path-exists? (fn [path] (secretary/locate-route path))})

)
