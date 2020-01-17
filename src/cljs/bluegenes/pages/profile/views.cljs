(ns bluegenes.pages.profile.views
  (:require [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [oget ocall]]
            [bluegenes.pages.profile.events :as events]
            [bluegenes.pages.profile.subs :as subs]
            [bluegenes.components.loader :refer [mini-loader]]))

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

(defn main []
  [:div.profile-page.container
   [user-preferences]
   [password-settings]])
