(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.progress_bar :as progress-bar]
            [bluegenes.route :as route]
            [bluegenes.components.ui.inputs :refer [password-input]]))

(defn mine-icon
  "returns the icon set for a specific mine, or a default.
   Pass it the entire set of mine details, e.g.
   (subscribe [:current-mine])."
  [mine]
  (let [icon (:icon mine)]
    [:svg.icon.logo {:class icon}
     [:use {:xlinkHref (str "#" icon)}]]))

(defn update-form [atom key evt]
  (swap! atom assoc key (oget evt :target :value)))

(defn mine-entry
  "Output a single mine in the mine picker"
  [mine-key details]
  [:li
   {:title (:description details)}
   [:a {:href (route/href ::route/home {:mine mine-key})}
    [:img {:src (:logo (:images details))}]
    (str (:name details)
         (when (= mine-key :default)
           " (default)"))]])

(defn mine-entry-current
  "Output a single mine in the mine picker"
  [details]
  [:li
   {:title (:description details)}
   [:a [mine-icon details]
    [:img {:src (:logo (:images details))}]
    (:name details) " (current)"]])

(defn settings
  "output the settings menu and mine picker"
  []
  (let [current-mine-name     @(subscribe [:current-mine-name])
        registry-with-default @(subscribe [:registry-with-default])]
    [:li.dropdown.mine-settings.secondary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-cog [:use {:xlinkHref "#icon-cog"}]]]
     (conj
      (into
       [:ul.dropdown-menu
        [mine-entry-current (get registry-with-default current-mine-name)]]
       (map (fn [[mine-key details]]
              ^{:key mine-key}
              [mine-entry mine-key details])
            (sort-by (comp :name val)
                     (dissoc registry-with-default current-mine-name))))
      [:li.special
       [:a {:href (route/href ::route/debug {:panel "main"})}
        ">_ Developer"]])]))

(defn logged-in []
  (let [identity (subscribe [:bluegenes.subs.auth/identity])]
    [:li.logon.dropdown.success.secondary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-circle"}]]
      [:span.long-name (str " " (:username @identity))]]
     [:ul.dropdown-menu
      [:li [:a {:href (route/href ::route/profile)} "Profile"]]
      [:li [:a {:on-click #(dispatch [:bluegenes.events.auth/logout])} "Log Out"]]]]))

(defn anonymous []
  (let [credentials    (reagent/atom {:username nil :password nil})
        register?      (reagent/atom false)
        current-mine   (subscribe [:current-mine])
        auth-values    (subscribe [:bluegenes.subs.auth/auth])]
    (fn []
      (let [{:keys [error? thinking? message]} @auth-values
            submit-fn #(dispatch [(if @register?
                                    :bluegenes.events.auth/register
                                    :bluegenes.events.auth/login)
                                  (assoc @credentials
                                         :service (:service @current-mine)
                                         :mine-id (:id @current-mine))])]
        [:li.logon.secondary-nav.dropdown.warning
         ;; Always show login dialog and not registration dialog, when first opened.
         {:ref (fn [evt] (some-> evt js/$
                                 (ocall :off "hide.bs.dropdown")
                                 (ocall :on  "hide.bs.dropdown"
                                        #(do (reset! register? false) nil))))}
         [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
          [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-times"}]] " Log In"]
         [:div.dropdown-menu.login-form-dropdown
          [:form.login-form
           [:h2 (str (if @register? "Create an account for " "Log in to ")
                     (:name @current-mine))]
           [:div.form-group
            [:label "Email Address"]
            [:input.form-control
             {:type "text"
              :id "email"
              :value (:username @credentials)
              :on-change (partial update-form credentials :username)
              :on-key-up #(when (= 13 (oget % :keyCode))
                            (submit-fn))}]]
           [password-input {:value (:password @credentials)
                            :on-change (partial update-form credentials :password)
                            :on-submit submit-fn}]
           [:div.register-or-login
            [:div.other-action
             [:a {:on-click #(do (dispatch [:bluegenes.events.auth/clear-error])
                                 (swap! register? not))}
              (if @register?
                "I already have an account"
                "I don't have an account")]]
            [:button.btn.btn-primary.btn-raised
             {:type "button"
              :on-click submit-fn}
             [mine-icon @current-mine]
             (if @register? "Register" "Sign In")]]
           (when error?
             [:div.alert.alert-danger.error-box message])]]]))))

(defn user []
  (let [authed? (subscribe [:bluegenes.subs.auth/authenticated?])]
    (fn []
      (if @authed?
        [logged-in @authed?]
        [anonymous]))))

(defn active-mine-logo []
  [mine-icon @(subscribe [:current-mine])])

(defn main []
  (let [active-panel (subscribe [:active-panel])
        current-mine (subscribe [:current-mine])
        panel-is (fn [panel-key] (= @active-panel panel-key))]
    (fn []
      [:nav#bluegenes-main-nav.main-nav
       [:ul
        [:li.minename.primary-nav
         [:a {:href (route/href ::route/home)}
          [active-mine-logo]
          [:span.long-name (:name @current-mine)]]]
        [:li.homelink.primary-nav.larger-screen-only
         {:class (if (panel-is :home-panel) "active")}
         [:a {:href (route/href ::route/home)}
          "Home"]]
        [:li.primary-nav {:class (when (panel-is :upload-panel) "active")}
         [:a {:href (route/href ::route/upload-step {:step "input"})}
          [:svg.icon.icon-upload.extra-tiny-screen [:use {:xlinkHref "#icon-upload"}]]
          [:span..long-name.larger-screen-only "Upload"]]]
        [:li.primary-nav {:class (when (panel-is :mymine-panel) "active")}
         [:a {:href (route/href ::route/mymine)}
          [:svg.icon.icon-cog [:use {:xlinkHref "#icon-my-data"}]]
          [:span "My\u00A0Data"]]]
        [:li.primary-nav {:class (when (panel-is :templates-panel) "active")}
         [:a {:href (route/href ::route/templates)}
          "Templates"]]
        ;;don't show region search for mines that have no example configured
        (when (:regionsearch-example @current-mine)
          [:li {:class (when (panel-is :regions-panel) "active")}
           [:a {:href (route/href ::route/regions)}
            "Regions"]])
        [:li.primary-nav {:class (when (panel-is :querybuilder-panel) "active")}
         [:a {:href (route/href ::route/querybuilder)}
          "Query\u00A0Builder"]]
        [:li.secondary-nav.search [search/main]]
        (when-not (panel-is :search-panel)
          [:li.secondary-nav.search-mini
           [:a {:href (route/href ::route/search)}
            [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]])
        [:li.secondary-nav.larger-screen-only
         [:a {:href (route/href ::route/help)}
          [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]]
        [settings]
        [user]]
       [progress-bar/main]])))
