(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [bluegenes.components.tooltip.views :as tooltip]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [ocall oget]]
            [bluegenes.components.progress_bar :as progress-bar]))

(defn update-form [atom key evt]
  (swap! atom assoc key (oget evt :target :value)))

(defn login-form []
  (let [credentials (reagent/atom {:username nil :password nil})]
    (fn [thinking?]
      [:form.login-form
       [:div.form-group
        [:label "Email Address"]
        [:input.form-control
         {:type "text"
          :value (:username @credentials)
          :on-change (partial update-form credentials :username)}]]
       [:div.form-group
        [:label "Password"]
        [:input.form-control
         {:type "password"
          :value (:password @credentials)
          :on-change (partial update-form credentials :password)
          :on-key-up (fn [k]
                       (when (= 13 (oget k :keyCode))
                         (dispatch [:bluegenes.events.auth/login @credentials])))}]]
       [:button.btn.btn-primary.btn-raised
        {:type "button"
         :on-click (fn [] (dispatch [:bluegenes.events.auth/login @credentials]))}
        "Sign In"]
       [:button.btn.btn-primary.btn-raised
        {:type "button"
         :on-click (fn [] (dispatch [:bluegenes.events.auth/login @credentials]))}
        "Google"]
       ;[:svg.icon.icon-spinner.spin [:use {:xlinkHref "#icon-spinner"}]]
       ])))

(defn mine-icon [mine]
  (let [icon (:icon mine)]
    [:svg.icon.logo {:class icon}
     [:use {:xlinkHref (str "#" icon)}]]))

(defn settings []
  (let [current-mine (subscribe [:current-mine])]
    (fn []
      [:li.dropdown.mine-settings
       [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:svg.icon.icon-cog [:use {:xlinkHref "#icon-cog"}]]]
       (conj (into [:ul.dropdown-menu]
                   (map (fn [[id details]]
                          [:li {:on-click (fn [e] (dispatch [:set-active-mine (keyword id)]))
                                :class (cond (= id (:id @current-mine)) "active")}
                           [:a [mine-icon details]
                            (:name details)]]) @(subscribe [:mines])))
             [:li.special [:a {:on-click #(navigate! "/debug")} ">_ Developer"]])])))

(defn logged-in []
  (let [identity (subscribe [:bluegenes.subs.auth/identity])]
    [:li.dropdown.success
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"}
      [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-circle"}]] (str " " (:username @identity))]
     [:ul.dropdown-menu
      [:li [:a {:on-click #(dispatch [:bluegenes.events.auth/logout])} "Log Out"]]]]))

(defn anonymous []
  [:li..dropdown.warning
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

(defn save-data-tooltip []
  (let [label (reagent/atom nil)]
    (reagent/create-class
      {:component-did-mount
       (fn [e] (reset! label (:label (reagent/props e))))
       :reagent-render
       (fn [tooltip-data]
         [tooltip/main
          {:content [:div.form-inline
                     [:label "Name: "
                      [:input.form-control
                       {:autofocus true
                        :type "text"
                        :on-change (fn [e] (reset! label (.. e -target -value)))
                        :placeholder @label}]]
                     [:button.btn "Save"]]
           :on-blur (fn []
                      (dispatch [:save-saved-data-tooltip (:id tooltip-data) @label]))}])})))

(defn active-mine-logo []
  [mine-icon @(subscribe [:current-mine])])

(defn main []
  (let [active-panel (subscribe [:active-panel])
        app-name     (subscribe [:name])
        short-name   (subscribe [:short-name])
        lists        (subscribe [:lists])
        ttip         (subscribe [:tooltip])
        current-mine (subscribe [:current-mine])
        panel-is     (fn [panel-key] (= @active-panel panel-key))]
    (fn []
      [:nav.navbar.navbar-default.navbar-fixed-top
       [:div.container-fluid
        [:ul.nav.navbar-nav.navbar-collapse.navigation
         [:li [:span.navbar-brand {:on-click #(navigate! "/")}
           [active-mine-logo]
           [:span.long-name (:name @current-mine)]]]
         [:li.homelink.larger-screen-only {:class (if (panel-is :home-panel) "active")} [:a {:on-click #(navigate! "/")} "Home"]]
         [:li {:class (if (panel-is :upload-panel) "active")} [:a {:on-click #(navigate! "/upload")}
          [:svg.icon.icon-upload.extra-tiny-screen [:use {:xlinkHref "#icon-upload"}]]
          [:span.larger-screen-only "Upload"]]]
         [:li {:class (if (panel-is :mymine-panel) "active")}
          [:a {:on-click #(navigate! "/mymine")}
           [:svg.icon.icon-cog [:use {:xlinkHref "#icon-user-circle"}]]
           [:span "My Data"]]]

         [:li {:class (if (panel-is :templates-panel) "active")} [:a {:on-click #(navigate! "/templates")} "Templates"]]

         ;;don't show region search for mines that have no example configured
         (cond (:regionsearch-example @current-mine)
               [:li {:class (if (panel-is :regions-panel) "active")} [:a {:on-click #(navigate! "/regions")} "Regions"]]
               )
         [:li {:class (if (panel-is :querybuilder-panel) "active")} [:a {:on-click #(navigate! "/querybuilder")} "Query\u00A0Builder"]]
         #_[:li {:class (if (panel-is :saved-data-panel) "active")} [:a {:on-click #(navigate! "/saved-data")} "Lists"[:span.larger-screen-only "\u00A0(" (apply + (map count (vals @lists))) ")"]]
          ;;example tooltip. Include as last child, probably with some conditional to display and an event handler for saving the name
          (if @ttip [save-data-tooltip @ttip])]
         ]
        [:ul.nav.navbar-nav.navbar-right.buttons
         [:li.search [search/main]]
         (cond (not (panel-is :search-panel)) [:li.search-mini [:a {:on-click #(navigate! "/search")} [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]])
         [:li.larger-screen-only [:a {:on-click #(navigate! "/help")} [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]]]]
         ;;This may have worked at some point in the past. We need to res it.
         [settings]
         [user]]]
       [progress-bar/main]])))
