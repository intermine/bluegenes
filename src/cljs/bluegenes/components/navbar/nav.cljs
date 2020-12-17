(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.progress_bar :as progress-bar]
            [bluegenes.route :as route]
            [bluegenes.components.ui.inputs :refer [password-input]]
            [bluegenes.components.icons :refer [icon-comp]]))

(def ^:const logo-path "/model/images/logo.png")

(defn mine-icon
  "returns the icon set for a specific mine, or a default.
   Pass it the entire set of mine details, e.g.
   (subscribe [:current-mine])."
  [details & {:keys [class]}]
  [:img
   {:class class
    :src (or (get-in details [:images :logo]) ; Path when it's from registry.
             (get-in details [:branding :images :logo]) ; Path when it's the current mine.
             (str (get-in details [:service :root]) logo-path))}]) ; Fallback path.

(defn update-form [atom key evt]
  (swap! atom assoc key (oget evt :target :value)))

(defn logged-in []
  (let [{:keys [username superuser]} @(subscribe [:bluegenes.subs.auth/identity])]
    [:li.logon.dropdown.success.primary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-2x.icon-user-circle [:use {:xlinkHref "#icon-user-circle"}]]
      [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
     [:ul.dropdown-menu.profile-dropdown
      [:li.email [:span username]]
      (when superuser
        [:li [:a {:href (route/href ::route/admin)} "Admin"]])
      [:li [:a {:href (route/href ::route/profile)} "Profile"]]
      [:li [:a {:on-click #(dispatch [:bluegenes.events.auth/logout])} "Logout"]]]]))

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
        [:li.logon.primary-nav.dropdown.warning
         ;; Always show login dialog and not registration dialog, when first opened.
         {:ref (fn [evt] (some-> evt js/$
                                 (ocall :off "hide.bs.dropdown")
                                 (ocall :on  "hide.bs.dropdown"
                                        #(do (reset! register? false) nil))))}
         [:a.dropdown-toggle
          {:data-toggle "dropdown" :role "button"}
          [:span "LOGIN"]
          [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
         [:div.dropdown-menu.login-form-dropdown
          [:form.login-form
           [:h2 (str (if @register? "Create an account for " "Login to ")
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
             [mine-icon @current-mine :class "mine-logo"]
             (if @register? "Register" "Login")]]
           (when error?
             [:div.alert.alert-danger.error-box message])]]]))))

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
  [mine-key details & {:keys [current?]}]
  [:li
   {:title (:description details)}
   [:a (if current?
         {:class "current"}
         {:href (route/href ::route/home {:mine mine-key})})
    [mine-icon details]
    (str (:name details)
         (when (= mine-key :default)
           " (default)"))]])

(defn mine-picker []
  (let [current-mine-name     @(subscribe [:current-mine-name])
        current-mine          @(subscribe [:current-mine])
        registry-with-default @(subscribe [:registry-with-default])]
    [:li.minename.mine-settings.dropdown.primary-nav
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [active-mine-logo current-mine]
      [:span.hidden-xs (:name current-mine)]
      [:svg.icon.icon-caret-down [:use {:xlinkHref "#icon-caret-down"}]]]
     (into [:ul.dropdown-menu.mine-picker]
           (map (fn [[mine-key details]]
                  ^{:key mine-key}
                  [mine-entry mine-key details
                   :current? (= mine-key current-mine-name)])
                (sort-by (comp :name val) registry-with-default)))]))

(def queries-to-show 10)

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
       [icon-comp "document-list" :enlarge 2]
       [icon-comp "caret-down"]]
      (into [:ul.dropdown-menu.results-dropdown]
            (let [queries @(subscribe [:results/historical-queries])]
              (for [[title {:keys [display-title]}] (take queries-to-show queries)]
                [:li
                 [:a {:on-click #(dispatch [::route/navigate ::route/results {:title title}])}
                  (or display-title title)]])))])
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
