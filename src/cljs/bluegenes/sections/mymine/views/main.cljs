(ns bluegenes.sections.mymine.views.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [bluegenes.sections.mymine.views.mymine :as mymine]))

(defn update-form [atom key evt]
  (swap! atom assoc key (oget evt :target :value)))

(defn login-form []
  (let [credentials (r/atom {:username nil :password nil})]
    (fn [thinking?]
      [:form
       [:div.form-group
        [:label "Username"]
        [:input.form-control
         {:type "text"
          :value (:username @credentials)
          :on-change (partial update-form credentials :username)}]]
       [:div.form-group
        [:label "Password"]
        [:input.form-control
         {:type "password"
          :value (:password @credentials)
          :on-change (partial update-form credentials :password)}]]
       [:button.btn.btn-primary.btn-raised
        {:type "button"
         :on-click (fn [] (dispatch [:bluegenes.events.auth/login @credentials]))}
        "Sign In"]
       ;[:svg.icon.icon-spinner.spin [:use {:xlinkHref "#icon-spinner"}]]
       ])))

(defn main []
  (fn []
    (let [{:keys [thinking? identity error? message]} @(subscribe [:bluegenes.subs.auth/auth])]
      [:div.container-fluid {:style {:width "100%" :margin 0 :padding 0}}
       (if (not-empty identity)
         [mymine/main]
         [:div
          [login-form thinking?]
          (when message [:div.alert.alert-danger message])])
       #_[mymine/main]])))


