(ns bluegenes.pages.profile.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget]]
            [bluegenes.pages.profile.events :as events]
            [bluegenes.pages.profile.subs :as subs]))

(defn password-settings []
  (let [new-password (r/atom "")
        response (subscribe [::subs/responses :change-password])]
    (fn []
      [:div.settings-group
       [:h3 "Change password"]
       [:div.form-group
        [:label "New password"]
        [:div.input-container
         [:input.form-control
          {:type "password"
           :value @new-password
           :autoComplete "new-password"
           :on-change #(reset! new-password (oget % :target :value))}]]
        [:div.flex-row
         [:button.btn.btn-primary.btn-raised
          {:type "button"
           :disabled (empty? @new-password)
           :on-click #(dispatch [::events/change-password @new-password])}
          "Save password"]
         (when-let [{:keys [type message]} @response]
           [:p {:class type} message])]]])))

(defn main []
  [:div.profile-page.container
   [password-settings]])

