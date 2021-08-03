(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.progress_bar :as progress-bar]
            [bluegenes.route :as route]
            [bluegenes.components.ui.inputs :refer [password-input]]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.time :as time]
            [clojure.string :as str]
            [bluegenes.config :refer [read-default-ns]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.utils :refer [get-mine-url]]))

(def ^:const logo-path "/model/images/logo.png")

;; This logo is banned! It's just the InterMine logo, and some mines
;; have it set as /branding:images.logo while they have a proper logo
;; under `logo-path`. I'll just sneak it in here and everything will
;; look nice :-)
(def ignore-logo-path "https://cdn.rawgit.com/intermine/design-materials/78a13db5/logos/intermine/squareish/45x45.png")

(defn mine-icon
  "returns the icon set for a specific mine, or a default.
   Pass it the entire set of mine details, e.g.
   (subscribe [:current-mine])."
  [details & {:keys [class]}]
  [:img
   {:class class
    :src
    (let [branding-path (or (get-in details [:images :logo]) ; Path when it's from registry.
                            (get-in details [:branding :images :logo])) ; Path when it's the current mine.
          fallback-path (str (get-mine-url details) logo-path)] ; Fallback path.
      (if (or (empty? branding-path)
              (= branding-path ignore-logo-path))
        fallback-path
        branding-path))}])

(defn update-form [atom key evt]
  (swap! atom assoc key (oget evt :target :value)))

(defn logged-in []
  (let [superuser? @(subscribe [:bluegenes.subs.auth/superuser?])
        email @(subscribe [:bluegenes.subs.auth/email])]
    [:li.logon.dropdown.success.primary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-2x.icon-user-circle [:use {:xlinkHref "#icon-user-circle"}]]
      [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
     [:ul.dropdown-menu.profile-dropdown
      [:li.email [:span email]]
      (when superuser?
        [:li [:a {:href (route/href ::route/admin)} "Admin"]])
      (when superuser?
        [:li [:a {:href (route/href ::route/tools)} "Tools"]])
      [:li [:a {:href (route/href ::route/profile)} "Profile"]]
      [:li [:a {:on-click #(dispatch [:bluegenes.events.auth/logout])} "Logout"]]]]))

(defn reset-password-form [{:keys [credentials on-back]}]
  (let [{:keys [error? thinking? message]
         success? :request-reset-success?} @(subscribe [:bluegenes.subs.auth/auth])
        current-mine @(subscribe [:current-mine])
        submit-fn #(dispatch [:bluegenes.events.auth/request-reset-password (:username @credentials)])]
    [:form.login-form
     [:h2 (str "Recover password on " (:name current-mine))]
     [:div.form-group
      [:label "Email"]
      [:input.form-control
       {:type "text"
        :id "email"
        :value (:username @credentials)
        :on-change (partial update-form credentials :username)
        :on-key-up #(when (= 13 (oget % :keyCode))
                      (submit-fn))}]]
     (cond
       error? [:div.alert.alert-danger.error-box message]
       success? [:div.alert.alert-success.error-box "Email with password recovery link has been sent"])
     [:button.btn.btn-primary.btn-raised.btn-block
      {:type "button"
       :on-click submit-fn}
      [mine-icon current-mine :class "mine-logo"]
      "Send recovery link"]
     [:a.btn-block.text-center
      {:role "button"
       :on-click #(do (dispatch [:bluegenes.events.auth/clear-error])
                      (on-back))}
      "Back to login"]]))

(defn register-form [{:keys [credentials on-back]}]
  (let [{:keys [error? thinking? message]} @(subscribe [:bluegenes.subs.auth/auth])
        current-mine @(subscribe [:current-mine])
        {:keys [username password]} @credentials
        submit-fn #(dispatch [:bluegenes.events.auth/register username password])]
    [:form.login-form
     [:h2 (str "Register on " (:name current-mine))]
     [:div.form-group
      [:label "Email"]
      [:input.form-control
       {:type "text"
        :id "email"
        :value username
        :on-change (partial update-form credentials :username)
        :on-key-up #(when (= 13 (oget % :keyCode))
                      (submit-fn))}]]
     [password-input {:value password
                      :on-change (partial update-form credentials :password)
                      :on-submit submit-fn}]
     (when error?
       [:div.alert.alert-danger.error-box message])
     [:button.btn.btn-primary.btn-raised.btn-block
      {:type "button"
       :on-click submit-fn}
      [mine-icon current-mine :class "mine-logo"]
      "Register"]
     [:a.btn-block.text-center
      {:role "button"
       :on-click #(do (dispatch [:bluegenes.events.auth/clear-error])
                      (on-back))}
      "Back to login"]]))

(defn login-form [{:keys [credentials on-reset-password on-register]}]
  (let [{:keys [error? thinking? message]} @(subscribe [:bluegenes.subs.auth/auth])
        oauth2-providers @(subscribe [:current-mine/oauth2-providers])
        current-mine @(subscribe [:current-mine])
        {:keys [username password]} @credentials
        submit-fn #(dispatch [:bluegenes.events.auth/login username password])]
    [:form.login-form
     [:h2 (str "Login to " (:name current-mine))]
     [:div.form-group
      [:label "Email"]
      [:input.form-control
       {:type "text"
        :id "email"
        :value username
        :on-change (partial update-form credentials :username)
        :on-key-up #(when (= 13 (oget % :keyCode))
                      (submit-fn))}]]
     [password-input {:value password
                      :on-change (partial update-form credentials :password)
                      :on-submit submit-fn}]
     (when error?
       [:div.alert.alert-danger.error-box message])
     [:button.btn.btn-primary.btn-raised.btn-block
      {:type "button"
       :on-click submit-fn}
      [mine-icon current-mine :class "mine-logo"]
      "Login"]
     [:a.btn-block.text-center
      {:role "button"
       :on-click #(do (dispatch [:bluegenes.events.auth/clear-error])
                      (on-reset-password))}
      "Forgot your password?"]
     [:a.btn-block.text-center
      {:role "button"
       :on-click #(do (dispatch [:bluegenes.events.auth/clear-error])
                      (on-register))}
      "Create new account"]
     [:div.oauth2-providers
      (when (contains? oauth2-providers "GOOGLE")
        [:button.btn
         {:type "button"
          :on-click #(dispatch [:bluegenes.events.auth/oauth2 "GOOGLE"])}
         [:img.google-signin
          {:src "/images/google-signin.png"
           :alt "[Sign in with Google]"}]])
      (when (contains? oauth2-providers "ELIXIR")
        [:button.btn
         {:type "button"
          :on-click #(dispatch [:bluegenes.events.auth/oauth2 "ELIXIR"])}
         [:img.elixir-login
          {:src "/images/elixir-login.png"
           :alt "[ELIXIR Login]"}]])]]))

(defn anonymous []
  (let [credentials (reagent/atom {:username nil :password nil})
        state* (reagent/atom :login)]
    (fn []
      [:li.logon.primary-nav.dropdown.warning
       ;; Always show login dialog and not registration dialog, when first opened.
       ;; Also clear any errors that may linger from a previous interaction.
       {:ref (fn [evt] (some-> evt js/$
                               (ocall :off "hide.bs.dropdown")
                               (ocall :on  "hide.bs.dropdown"
                                      #(do (reset! state* :login)
                                           (dispatch [:bluegenes.events.auth/clear-error])
                                           nil))))}
       [:a.dropdown-toggle
        {:data-toggle "dropdown" :role "button"}
        [:span "LOGIN"]
        [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
       [:div.dropdown-menu.login-form-dropdown
        (case @state*
          :login [login-form {:credentials credentials
                              :on-reset-password #(reset! state* :reset-password)
                              :on-register #(reset! state* :register)}]
          :register [register-form {:credentials credentials
                                    :on-back #(reset! state* :login)}]
          :reset-password [reset-password-form {:credentials credentials
                                                :on-back #(reset! state* :login)}])]])))

(defn user []
  (let [authed? (subscribe [:bluegenes.subs.auth/authenticated?])]
    (fn []
      (if @authed?
        [logged-in @authed?]
        [anonymous]))))

(defn active-mine-logo [current-mine]
  (let [logo    (get-in current-mine [:branding :images :logo])
        service (get-in current-mine [:service :root])]
    [:img.active-mine-image
     {:src (or logo (str service logo-path))}]))

(defn mine-entry
  "Output a single mine in the mine picker"
  [mine-key details & {:keys [current? external?]}]
  [:li
   (when-let [desc (not-empty (:description details))]
     {:title desc})
   [:a (cond
         current? {:class "current"}
         external? {:target "_blank" :href (get-mine-url details)}
         :else {:href (route/href ::route/home {:mine mine-key})})
    [mine-icon details]
    (str (:name details)
         (when (= mine-key (read-default-ns))
           " (default)"))
    (when external?
      [icon "external"])]])

(defn mine-picker []
  (let [current-mine-name @(subscribe [:current-mine-name])
        current-mine @(subscribe [:current-mine])
        registry @(subscribe [:registry-wo-configured-mines])
        registry-external @(subscribe [:registry-external])
        configured-mines @(subscribe [:env/mines])
        map-mines (fn [mines & {:keys [external?]}]
                    (map (fn [[mine-key details]]
                           ^{:key mine-key}
                           [mine-entry mine-key details
                            :current? (= mine-key current-mine-name)
                            :external? (or external? (:external? details))])
                         (sort-by (comp :name val) mines)))]
    [:li.minename.mine-settings.dropdown.primary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [active-mine-logo current-mine]
      [:span.hidden-xs (:name current-mine)]
      [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
     (into [:ul.dropdown-menu.mine-picker]
           (concat (map-mines configured-mines)
                   (when (seq registry)
                     [[:li.header [:h4 "Registry mines"]]])
                   (map-mines registry)
                   (when (seq registry-external)
                     [[:li.header [:h4 "Incompatible mines"
                                   [poppable {:data "These mines are incompatible due to either running an InterMine API version below 27 or being only available through unsecured HTTP when BlueGenes is accessed through HTTPS. Selecting any of them will open a new tab."
                                              :children [icon "question"]}]]]])
                   (map-mines registry-external :external? true)))]))

(def queries-to-show 5)

(defn nav-buttons [classes & {:keys [large-screen?]}]
  [:<>
   [:li.primary-nav.hidden-xs
    {:class (classes :home-panel large-screen?)}
    [:a {:href (route/href ::route/home)}
     "Home"]]
   [:li.primary-nav
    {:class (classes :upload-panel large-screen?)}
    [:a {:href (route/href ::route/upload-step {:step "input"})}
     "Upload"]]
   [:li.primary-nav
    {:class (classes :lists-panel large-screen?)}
    [:a {:href (route/href ::route/lists)}
     "Lists"]]
   [:li.primary-nav
    {:class (classes :templates-panel large-screen?)}
    [:a {:href (route/href ::route/templates)}
     "Templates"]]
   [:li.primary-nav
    {:class (classes :regions-panel large-screen?)}
    [:a {:href (route/href ::route/regions)}
     "Regions"]]
   [:li.primary-nav
    {:class (classes :querybuilder-panel large-screen?)}
    [:a {:href (route/href ::route/querybuilder)}
     "Query\u00A0Builder"]]
   (when @(subscribe [:results/have-been-queries?])
     [:li.queries-container.hidden-xs.hidden-sm
      {:class (classes :results-panel large-screen?)}
      [:a.dropdown-toggle.queries-button
       {:data-toggle "dropdown" :role "button"}
       ;; This has the same height as the *visible* icon, so it ensures the icon
       ;; in the middle is centered.
       [icon-comp "caret-down" :classes [:invisible]]
       [:span "Activity"]
       [icon-comp "caret-down"]]
      (into [:ul.dropdown-menu.results-dropdown.list-group
             [:li.list-group-item.results-heading
              [:div.list-group-item-content
               [:h4.list-group-item-heading "Recent activity"]]]]
            (let [queries @(subscribe [:results/historical-queries])]
              (for [[title {:keys [display-title intent] :as query}] (take queries-to-show queries)]
                [:li.list-group-item
                 [:a.list-group-item-content
                  {:on-click #(dispatch [::route/navigate ::route/results {:title title}])}
                  [:div.list-group-item-heading (or display-title title)]
                  [:div.list-group-item-text
                   (time/format-query query)
                   (when intent
                     (str " - " (-> intent name str/capitalize)))]]])))])
   [:li.primary-nav.hidden-md.hidden-lg
    {:class (classes :search-panel large-screen?)}
    [:a {:href (route/href ::route/search)}
     "Search"]]])

(defn main []
  (let [active-panel (subscribe [:active-panel])
        main-color (subscribe [:branding/header-main])
        text-color (subscribe [:branding/header-text])
        classes (fn [panel-key large-screen?]
                  [(when (= @active-panel panel-key) "active")
                   (when large-screen? "hidden-xs")])]
    (fn []
      [:nav#bluegenes-main-nav.main-nav
       {:style {:background-color @main-color
                :color @text-color
                :fill @text-color}}
       [:ul
        [:li.primary-nav.bluegenes-logo
         [:a {:href (route/href ::route/home)}
          [:svg.icon.icon-3x.icon-bluegenes-logo
           [:use {:xlinkHref "#icon-bluegenes-logo"}]]
          [:span.hidden-xs.hidden-sm "BLUEGENES"]]]
        ;; We want to show the nav buttons inside a container on small screens,
        ;; so it can be scrolled. But we don't want the nav buttons inside the
        ;; container on larger screens, so they can be placed more spaciously.
        ;; This is how we achieve this!
        [nav-buttons classes :large-screen? true]
        [:div.nav-links.hidden-sm.hidden-md.hidden-lg
         [nav-buttons classes]]
        [:li.primary-nav.search.hidden-xs.hidden-sm [search/main]]
        [mine-picker]
        [user]]])))
