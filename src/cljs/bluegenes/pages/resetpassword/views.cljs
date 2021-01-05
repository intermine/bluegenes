(ns bluegenes.pages.resetpassword.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget]]
            [bluegenes.components.ui.inputs :refer [password-input]]))

(defn main []
  (let [new-password (r/atom "")]
    (fn []
      (let [{:keys [token]} @(subscribe [:panel-params])
            submit-fn #(dispatch [:bluegenes.events.auth/reset-password @new-password token])]
        [:div.container.reset-password-page
         [:div.row
          [:div.col-xs-8.col-xs-offset-2
           [:div.well.well-lg
            [:h2 "Reset password"]
            [password-input {:label "New password"
                             :value @new-password
                             :on-change #(reset! new-password (oget % :target :value))
                             :on-submit submit-fn
                             :container-class "reset-password-new"
                             :new-password? true}]
            [:button.btn.btn-primary.btn-raised
             {:type "button"
              :on-click submit-fn}
             "Reset password"]]]]]))))
