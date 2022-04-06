(ns bluegenes.pages.resetpassword.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget]]
            [bluegenes.components.ui.inputs :refer [password-input]]
            [bluegenes.components.icons :refer [icon]]))

(defn main []
  (let [new-password (r/atom "")]
    (fn []
      (let [{:keys [token]} @(subscribe [:panel-params])
            {in-progress? :reset-password-in-progress?
             success? :reset-password-success?
             error :reset-password-error} @(subscribe [:bluegenes.subs.auth/auth])
            submit-fn #(dispatch [:bluegenes.events.auth/reset-password @new-password token])]
        [:div.container.reset-password-page
         [:div.row
          [:div.col-xs-8.col-xs-offset-2
           (if success?
             [:div.reset-password-success
              [icon "checkmark"]
              [:h3 "Your password has been changed."]
              [:p "Please use the " [:strong "Login"] " button at the top-right to login with your new password."]]
             [:div.well.well-lg
              [:h2 "Reset password"]
              [password-input {:label "New password"
                               :value @new-password
                               :on-change #(reset! new-password (oget % :target :value))
                               :on-submit submit-fn
                               :container-class "reset-password-new"
                               :new-password? true}]
              (when error
                [:div.alert.alert-danger error])
              [:button.btn.btn-primary.btn-raised
               {:type "button"
                :disabled in-progress?
                :on-click submit-fn}
               "Reset password"]])]]]))))
