(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [oops.core :refer [oget]]
            [bluegenes.components.progress_bar :as progress-bar]
            [bluegenes.route :as route]))

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

(defn register-for-mine
  "Note the iframe hack we've included below to ensure the user actually
  reaches the registration page, rather than the homepage :(
  I'm sure in the future we'll remove this when we have auth set up fully.
  The dangerouslysetInnerHTML comment is intentional as a hidden iframe looks
  mega spooky to anyone inspecting source. Please don't remove the comment unless it's because we've got better auth working!!"
  [current-mine]
  (let [link (str (get-in @current-mine [:service :root]) "/createAccount.do")]
    [:div.register
     [:div.sneaky-iframe-fix-see-comment
      {:dangerouslySetInnerHTML {:__html (str "<!-- InterMine automatically redirects to the homepage unless you have a session (sigh) - but we want the user to go to the registration page. So we're loading the page in an iframe the user can't see, to bootstrap the session :/ -->")}}]
     [:iframe.forceregistrationscreen {:src link :height "0px" :width "0px"}]
     [:a {:href link} "Register"]]))

(defn login-form []
  (let [credentials (reagent/atom {:username nil :password nil})
        current-mine (subscribe [:current-mine])
        auth-values (subscribe [:bluegenes.subs.auth/auth])]
    (fn []
      (let [{:keys [error? thinking?]} @auth-values]
        [:form.login-form
         [:h2 "Log in to " (:name @current-mine)]
         [:div.form-group
          [:label "Email Address"]
          [:input.form-control
           {:type "text"
            :id "email"
            :value (:username @credentials)
            :on-change (partial update-form credentials :username)}]]
         [:div.form-group
          [:label "Password"]
          [:input.form-control
           {:type "password"
            :id "password"
            :value (:password @credentials)
            :on-change (partial update-form credentials :password)
            :on-key-up (fn [k]
                         (when (= 13 (oget k :keyCode))
                           (dispatch [:bluegenes.events.auth/login
                                      (assoc @credentials
                                             :service (:service @current-mine)
                                             :mine-id (:id @current-mine))])))}]]
         [:div.register-or-login
          [register-for-mine current-mine]
          [:button.btn.btn-primary.btn-raised
           {:type "button"
            :on-click (fn []
                        (dispatch [:bluegenes.events.auth/login
                                   (assoc @credentials
                                          :service (:service @current-mine)
                                          :mine-id (:id @current-mine))]))}
           [mine-icon @current-mine]
           "Sign In"]]
         (when error?
           [:div.alert.alert-danger.error-box
            "Invalid username or password"])]))))

(defn mine-entry [details current-mine?]
  "Output a single mine in the mine picker"
  [:li
   {:class (when current-mine? "active")
    :title (:description details)}
   [:a {:href (route/href ::route/home {:mine (-> details :namespace keyword)})}
    (if current-mine?
      [mine-icon details]
      [:img {:src (:logo (:images details))}])
    (:name details)
    (when current-mine? " (current)")]])

(defn mine-entry-current [details]
  "Output a single mine in the mine picker"
  [:li
   [:a [mine-icon details]
    [:img {:src (:logo (:images details))}]
    (:name details) " (current)"]])

(defn settings []
  "output the settings menu and mine picker"
  (let [current-mine (subscribe [:current-mine])]
    (fn []
      [:li.dropdown.mine-settings.secondary-nav
       [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
        [:svg.icon.icon-cog [:use {:xlinkHref "#icon-cog"}]]]
       (conj
        (into
         [:ul.dropdown-menu
          [mine-entry-current @current-mine]]
         (map (fn [[id details]]
                [mine-entry details]) @(subscribe [:registry])))
        [:li.special
         [:a {:href (route/href ::route/debug {:panel "main"})}
          ">_ Developer"]])])))

(defn logged-in []
  (let [identity (subscribe [:bluegenes.subs.auth/identity])]
    [:li.logon.dropdown.success.secondary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-circle"}]]
      [:span.long-name (str " " (:username @identity))]]
     [:ul.dropdown-menu
      [:li [:a {:on-click #(dispatch [:bluegenes.events.auth/logout])} "Log Out"]]]]))

(defn anonymous []
  [:li.logon.secondary-nav.dropdown.warning
   [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
    [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-times"}]] " Log In"]
   [:div.dropdown-menu.login-form-dropdown
    [login-form]]])

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
