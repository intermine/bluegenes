(ns bluegenes.sections.mymine.views.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [bluegenes.sections.mymine.views.mymine :as mymine]))



(defn main []
  (fn []
    (let [{:keys [thinking? identity error? message]} @(subscribe [:bluegenes.subs.auth/auth])]
      [:div.container-fluid {:style {:width "100%" :margin 0 :padding 0}}
       #_(if (not-empty identity)
         [mymine/main]
         [:div
          [login-form thinking?]
          (when message [:div.alert.alert-danger message])])
       [mymine/main]])))


