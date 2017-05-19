(ns bluegenes.components.navbar.nav
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.search.typeahead :as search]
            [bluegenes.components.tooltip.views :as tooltip]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [ocall]]
            [bluegenes.components.progress_bar :as progress-bar]))

(defn mine-icon [mine]
  (let [icon (:icon mine)]
  [:svg.icon.logo {:class icon}
    [:use {:xlinkHref (str "#" icon) }]
   ]))


(defn settings []
  (let [current-mine (subscribe [:current-mine])]
  (fn []
    [:li.dropdown.mine-settings
     [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-cog]]
      (conj (into [:ul.dropdown-menu]
        (map (fn [[id details]]
          [:li {:on-click (fn [e] (dispatch [:set-active-mine (keyword id)]))
           :class (cond (= id (:id @current-mine)) "active")}
            [:a [mine-icon details]
                 (:name details)]]) @(subscribe [:mines])))
        [:li.special [:a {:on-click #(navigate! "/debug")} [:i.fa.fa-terminal] " Developer"]])
])))

(defn logged-in [user]
  [:li.dropdown.active
   [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-user]]
   [:ul.dropdown-menu
    [:div.logged-in
     [:i.fa.fa-check-circle.fa-3x] (str (:username user))]
    [:li [:a {:on-click #(dispatch [:log-out])} "Log Out"]]]])

(defn anonymous []
  [:li.dropdown
   [:a.dropdown-toggle {:data-toggle "dropdown" :role "button"} [:i.fa.fa-user-times]]
   [:ul.dropdown-menu
    [:li [:a {:on-click #(dispatch [:log-in])} "Log In"]]]])

(defn user []
  (let [who-am-i (subscribe [:who-am-i])]
    (fn []
      (if @who-am-i
        [logged-in @who-am-i]
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
                       {:autofocus   true
                        :type        "text"
                        :on-change   (fn [e] (reset! label (.. e -target -value)))
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
       [:div.navbar-header
         [:span.navbar-brand {:on-click #(navigate! "/")}
           [active-mine-logo]
           [:span.long-name (:name @current-mine)]]]
        [:ul.nav.navbar-nav.navbar-collapse.navigation
         [:li.homelink {:class (if (panel-is :home-panel) "active")} [:a {:on-click #(navigate! "/")} "Home"]]
         [:li {:class (if (panel-is :upload-panel) "active")} [:a {:on-click #(navigate! "/upload")} "Upload"]]
         [:li {:class (if (panel-is :templates-panel) "active")} [:a {:on-click #(navigate! "/templates")} "Templates"]]

         ;;don't show region search for mines that have no example configured
         (cond (:regionsearch-example @current-mine)
           [:li {:class (if (panel-is :regions-panel) "active")} [:a {:on-click #(navigate! "/regions")} "Regions"]]
         )
         [:li {:class (if (panel-is :querybuilder-panel) "active")} [:a {:on-click #(navigate! "/querybuilder")} "Query\u00A0Builder"]]
         [:li {:class (if (panel-is :saved-data-panel) "active")} [:a {:on-click #(navigate! "/saved-data")} (str "Lists\u00A0(" (apply + (map count (vals @lists))) ")")]
          ;;example tooltip. Include as last child, probably with some conditional to display and an event handler for saving the name
          (if @ttip [save-data-tooltip @ttip])]]
        [:ul.nav.navbar-nav.navbar-right.buttons
         [:li.search [search/main]]
         (cond (not (panel-is :search-panel)) [:li.search-mini [:a {:on-click #(navigate! "/search")} [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]])
         [:li [:a {:on-click #(navigate! "/help")} [:i.fa.fa-question]]]
         ;;This may have worked at some point in the past. We need to res it.
        ; [user]
         [settings]]]
       [progress-bar/main]])))
