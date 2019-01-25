(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [oget]]
            [bluegenes.components.progress_bar :as progress-bar]))

(defn mine-icon
  "returns the icon set for a specific mine, or a default.
   Pass it the entire set of mine details, e.g.
   @(subscribe [:current-mine])."
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
  (let [link (str "//"
                  (get-in @current-mine [:mine :url]) "/createAccount.do")]
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
           [:div.alert.alert-danger.error-box "Invalid username or password"])]))))

(defn settings []
  (let [current-mine (subscribe [:current-mine])]
    (fn []
      [:li.dropdown.mine-settings.secondary-nav
       [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:svg.icon.icon-cog [:use {:xlinkHref "#icon-cog"}]]]
       (conj (into [:ul.dropdown-menu]
                   (map (fn [[id details]]
                          [:li {:on-click (fn [e] (dispatch [:set-active-mine (keyword id)]))
                                :class (cond (= id (:id @current-mine)) "active")}
                           [:a [mine-icon details]
                            (if (= :default id)
                              (clojure.string/join " - " [(:name details) "Default"])
                              (:name details))]]) @(subscribe [:mines])))
             [:li.special [:a {:on-click #(navigate! "/debug/main")} ">_ Developer"]])])))

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
        [:li.minename.primary-nav {:on-click #(navigate! "/")}
         [active-mine-logo]
         [:span.long-name (:name @current-mine)]]
        [:li.homelink.primary-nav.larger-screen-only {:class (if (panel-is :home-panel) "active")} [:a {:on-click #(navigate! "/")} "Home"]]
        [:li.primary-nav {:class (if (panel-is :upload-panel) "active")}
         [:a {:on-click #(navigate! "/upload/input")}
          [:svg.icon.icon-upload.extra-tiny-screen [:use {:xlinkHref "#icon-upload"}]]
          [:span..long-name.larger-screen-only "Upload"]]]
        [:li.primary-nav {:class (if (panel-is :mymine-panel) "active")}
         [:a {:on-click #(navigate! "/mymine")}
          [:svg.icon.icon-cog [:use {:xlinkHref "#icon-my-data"}]]
          [:span "My\u00A0Data"]]]
        [:li.primary-nav {:class (if (panel-is :templates-panel) "active")} [:a {:on-click #(navigate! "/templates")} "Templates"]]
        ;;don't show region search for mines that have no example configured
        (cond (:regionsearch-example @current-mine)
              [:li {:class (if (panel-is :regions-panel) "active")} [:a {:on-click #(navigate! "/regions")} "Regions"]])
        [:li.primary-nav {:class (if (panel-is :querybuilder-panel) "active")} [:a {:on-click #(navigate! "/querybuilder")} "Query\u00A0Builder"]]
        [:li.secondary-nav.search [search/main]]
        (cond (not (panel-is :search-panel)) [:li.secondary-nav.search-mini [:a {:on-click #(navigate! "/search")} [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]])
        [:li.secondary-nav.larger-screen-only [:a {:on-click #(navigate! "/help")} [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]]
        [settings]
        [user]]
       [progress-bar/main]])))
