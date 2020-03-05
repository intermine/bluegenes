(ns bluegenes.pages.profile.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget ocall]]
            [bluegenes.pages.profile.events :as events]
            [bluegenes.pages.profile.subs :as subs]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.ui.inputs :refer [password-input]]))

(defn password-settings []
  (let [old-password (r/atom "")
        new-password (r/atom "")
        response (subscribe [::subs/responses :change-password])
        submit-fn #(dispatch [::events/change-password @old-password @new-password])]
    (fn []
      [:div.settings-group
       [:h3 "Change password"]
       [password-input {:value @old-password
                        :on-change #(reset! old-password (oget % :target :value))
                        :on-submit submit-fn
                        :container-class "input-container"
                        :label "Old password"}]
       [password-input {:value @new-password
                        :on-change #(reset! new-password (oget % :target :value))
                        :on-submit submit-fn
                        :container-class "input-container"
                        :new-password? true
                        :label "New password"}]
       [:div.save-button.flex-row
        [:button.btn.btn-primary.btn-raised
         {:type "button"
          :disabled (some empty? [@old-password @new-password])
          :on-click submit-fn}
         "Save password"]
        (when-let [{:keys [type message]} @response]
          [:p {:class type} message])]])))

(defn input [{:keys [group id label type] :or {type "text"}}]
  [:div.form-group
   [:label {:for (str "input" id)} label]
   [:div.input-container
    [:input.form-control
     {:type type
      :id (str "input" id)
      :value @(subscribe [::subs/inputs [group id]])
      :on-change #(dispatch [::events/set-input [group id] (oget % :target :value)])}]]])

(defn checkbox [{:keys [group id label reversed?]}]
  [:div.form-group
   [:label label
    [:input {:type "checkbox"
             :checked (cond-> @(subscribe [::subs/inputs [group id]]) reversed? not)
             :on-change #(dispatch [::events/update-input [group id] not])}]]])

(defn user-preferences []
  (let [requesting? (subscribe [::subs/requests :get-preferences])
        response (subscribe [::subs/responses :user-preferences])
        loader-ref (atom nil)
        ;; It's possible to do this with CSS transitions, except the dark
        ;; background will abruptly appear and disappear even though the
        ;; request is so quick the transition doesn't have time to finish.
        loader-timeout (when @requesting?
                         (js/setTimeout
                          #(some-> (js/$ @loader-ref)
                                   (ocall :addClass "is-loading"))
                          500))]
    (fn []
      (when (not @requesting?)
        (js/clearTimeout loader-timeout)
        (some-> (js/$ @loader-ref) (ocall :removeClass "is-loading")))
      [:div.settings-group
       [:div.settings-loader {:ref #(reset! loader-ref %)}
        [mini-loader]]
       [:h3 "User preferences"]
       [checkbox {:id :do_not_spam
                  :group :user-preferences
                  :reversed? true
                  :label "Inform me by email of newly shared lists"}]
       [checkbox {:id :hidden
                  :group :user-preferences
                  :reversed? true
                  :label "Allow other users to share lists with me without confirmation"}]
       [input {:id :alias
               :group :user-preferences
               :label "Public name (use this name to share lists with me)"}]
       [input {:id :email
               :group :user-preferences
               :type "email"
               :label "My preferred email address"}]
       [input {:id :galaxy-url
               :group :user-preferences
               :label "The URL of your preferred Galaxy instance"}]
       (let [input-prefs @(subscribe [::subs/inputs [:user-preferences]])
             saved-prefs @(subscribe [::subs/preferences])]
         [:div.save-button.flex-row
          [:button.btn.btn-primary.btn-raised
           {:type "button"
            :disabled (= input-prefs saved-prefs)
            :on-click #(dispatch [::events/save-preferences input-prefs])}
           "Save changes"]
          (when-let [{:keys [type message]} @response]
            [:p {:class type} message])])])))

(defn delete-account []
  (let [deregistration-token (subscribe [::subs/responses :deregistration-token])
        mine-name (subscribe [:current-mine-human-name])
        deregistration-input (r/atom "")]
    (fn []
      [:div.settings-group
       [:h3 "Delete account"]
       (if (not-empty @deregistration-token)
         [:<>
          [:p "Copy the following code into the input field below, then press the button to delete your account on " [:strong @mine-name] "."
           [:br]
           [:code @deregistration-token]]
          [:div.alert.alert-danger
           "Once completed, you will no longer be able to login to your account and access your saved lists, queries, templates, tags and user preferences on this InterMine instance."
           [:br]
           [:strong "THIS CANNOT BE UNDONE."]]
          [:div.form-group
           [:label "Code for account deletion"]
           [:div.input-container
            [:input.form-control
             {:type "text"
              :value @deregistration-input
              :on-change #(reset! deregistration-input (oget % :target :value))}]]
           [:div.flex-row
            [:button.btn.btn-danger.btn-raised
             {:type "button"
              :disabled @(subscribe [::subs/requests :delete-account])
              :on-click #(dispatch [::events/delete-account @deregistration-input])}
             "Delete account"]
            (when-let [{:keys [type message]} @(subscribe [::subs/responses :delete-account])]
              [:p {:class type} message])]]]
         [:<>
          [:p "Delete your account on " [:strong @mine-name] " including all your saved lists, queries, templates, tags and user preferences on this InterMine instance."]
          [:div.save-button.flex-row
           [:button.btn.btn-danger.btn-raised
            {:type "button"
             :disabled @(subscribe [::subs/requests :start-deregistration])
             :on-click #(dispatch [::events/start-deregistration])}
            "Start account deletion"]
           (when-let [{:keys [type message]} @(subscribe [::subs/responses :deregistration])]
             [:p {:class type} message])]])])))

(defn main []
  [:div.profile-page.container
   [user-preferences]
   [password-settings]
   [delete-account]])
