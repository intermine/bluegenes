(ns bluegenes.pages.profile.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget ocall]]
            [bluegenes.pages.profile.events :as events]
            [bluegenes.pages.profile.subs :as subs]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.ui.inputs :refer [password-input]]
            [bluegenes.version :refer [proper-login-support]]))

(defn generate-api-key []
  (let [bg-uses-api-key? (< @(subscribe [:api-version]) proper-login-support)
        response @(subscribe [::subs/responses :generate-api-key])]
    [:div.settings-group
     [:h3 "API access key"]
     [:p "You can access the features of the InterMine API securely using an API access key. A key uniquely identifies you to the webservice, without requiring you to transmit your username or password. At any time you can change, or delete your API key, without having to change your password. If you do not yet have an API key, click on the button below to generate a new token."]
     [:p [:strong "Note: "] "Generating a new API key will invalidate any existing one. If you wish to reuse an API key, you should save it in a safe place. You will only be able to view the API key for the length of this session."]
     (if bg-uses-api-key?
       [:pre.token-box @(subscribe [:active-token])]
       [:pre.token-box (or @(subscribe [::subs/api-key]) "-")])
     [:div.save-button.flex-row
      [:button.btn.btn-primary.btn-raised
       {:type "button"
        :disabled (or bg-uses-api-key? @(subscribe [::subs/requests :generate-api-key]))
        :on-click #(dispatch [::events/generate-api-key])}
       "Generate a new API key"]
      (if bg-uses-api-key?
        [:p.failure "Generating a new API key is not supported in this version of InterMine."]
        (when-let [{:keys [type message]} response]
          [:p {:class type} message]))]]))

(defn password-settings []
  (let [old-password (r/atom "")
        new-password (r/atom "")
        response (subscribe [::subs/responses :change-password])
        submit-fn #(dispatch [::events/change-password @old-password @new-password])
        oauth2? (subscribe [:bluegenes.subs.auth/oauth2?])]
    (fn []
      [:div.settings-group
       [:h3 "Change password"]
       (when @oauth2?
         [:p [:code "You are logged in through an external authentication provider. Please use their services to change your password."]])
       [password-input {:value @old-password
                        :on-change #(reset! old-password (oget % :target :value))
                        :on-submit submit-fn
                        :container-class "input-container"
                        :label "Old password"
                        :disabled @oauth2?}]
       [password-input {:value @new-password
                        :on-change #(reset! new-password (oget % :target :value))
                        :on-submit submit-fn
                        :container-class "input-container"
                        :new-password? true
                        :label "New password"
                        :disabled @oauth2?}]
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
   [generate-api-key]
   [password-settings]
   [delete-account]])
