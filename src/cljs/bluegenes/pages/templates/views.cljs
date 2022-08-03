(ns bluegenes.pages.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as s :refer [split join blank?]]
            [json-html.core :as json-html]
            [bluegenes.components.lighttable :as lighttable]
            [imcljs.path :as im-path]
            [bluegenes.components.ui.constraint :refer [constraint]]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.utils :refer [ascii-arrows ascii->svg-arrows compatible-version?]]
            [bluegenes.pages.templates.helpers :refer [categories-from-tags]]
            [bluegenes.components.top-scroll :as top-scroll]
            [bluegenes.route :as route]
            [bluegenes.components.icons :refer [icon]]
            [goog.functions :refer [debounce]]
            [bluegenes.components.bootstrap :refer [poppable]]))

(defn categories []
  (let [categories (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills.template-categories
             [:li {:on-click #(dispatch [:template-chooser/set-category-filter nil])
                   :class (if (nil? @selected-category) "active")}
              [:a.type-all "All"]]]
            (map (fn [category]
                   [:li {:on-click #(dispatch [:template-chooser/set-category-filter category])
                         :class
                         (if (= category @selected-category) " active")}
                    [:a {:class (str
                                 "type-" category)} category]])
                 @categories)))))

(def css-transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransitionGroup))

(defn web-service-url []
  (let [input-ref* (atom nil)
        url (subscribe [:template-chooser/web-service-url])]
    (fn []
      [:span.dropdown
       [:a.dropdown-toggle.action-button
        {:data-toggle "dropdown"
         :role "button"}
        [icon "file"] "Web service URL"]
       [:div.dropdown-menu
        [:form {:on-submit #(.preventDefault %)}
         [:div.web-service-url-container
          [:p "Use the URL below to fetch the first " [:strong "10"] " records for this template from the command line or a script " [:em "(authentication needed for private templates and lists)"] " :"]
          [:input.form-control
           {:type "text"
            :ref (fn [el]
                   (when el
                     (reset! input-ref* el)
                     (.focus el)
                     (.select el)))
            :autoFocus true
            :readOnly true
            :value @url}]
          [:button.btn.btn-raised
           {:on-click (fn [_]
                        (when-let [input-el @input-ref*]
                          (.focus input-el)
                          (.select input-el)
                          (try
                            (ocall js/document :execCommand "copy")
                            (catch js/Error _))))}
           "Copy"]]]]])))

(defn preview-results
  "Preview results of template as configured by the user or default config"
  []
  (let [authed? @(subscribe [:bluegenes.subs.auth/authenticated?])
        fetching-preview? @(subscribe [:template-chooser/fetching-preview?])
        results-preview @(subscribe [:template-chooser/results-preview])
        preview-error @(subscribe [:template-chooser/preview-error])
        changed-selected? @(subscribe [:template-chooser/changed-selected?])
        loading? (if preview-error
                   false
                   fetching-preview?)
        results-count (:iTotalRecords results-preview)
        ;; This differs from authed? in that it's whether the logged in user is
        ;; authorized to delete the template.
        authorized? @(subscribe [:template-chooser/authorized?])]
    [:div.col-xs-8.preview
     [:div.preview-header
      [:h4 "Results Preview"]
      (when loading?
        [:div.preview-header-loader
         [mini-loader "tiny"]])]
     [:div.preview-table-container
      (cond
        preview-error [:div
                       [:pre.well.text-danger preview-error]]
        :else [preview-table
               :query-results results-preview
               :loading? loading?])]
     [:div.btn-group
      [:button.btn.btn-primary.btn-raised.view-results
       {:type "button"
        :on-click (fn [] (dispatch [:templates/send-off-query]))}
       (if (or loading? preview-error (< results-count 1))
         "Open in results page"
         (str "View "
              results-count
              (if (> results-count 1) " rows" " row")))]
      [:button.btn.btn-default.btn-raised
       {:type "button"
        :disabled (not changed-selected?)
        :on-click (fn [] (dispatch [:templates/reset-template]))}
       "Reset"]
      [:button.btn.btn-default.btn-raised
       {:type "button"
        :on-click (fn [] (dispatch [:templates/edit-query]))}
       "Edit query"]
      (when authed?
        [:button.btn.btn-default.btn-raised
         {:type "button"
          :on-click (fn [] (dispatch [:templates/edit-template]))}
         "Edit template"])
      (when authorized?
        [:button.btn.btn-link
         {:type "button"
          :on-click (fn [] (dispatch [:templates/delete-template]))}
         [icon "bin"]])]
     [:div.more-actions
      [web-service-url]]]))

(defn toggle []
  (fn [{:keys [status on-change]}]
    [:div.switch-container
     [:span.switch-label "Optional"]
     [:span.switch
      [:input {:type "checkbox" :checked (case status "ON" true "OFF" false false)}]
      [:span.slider.round {:on-click on-change}]]
     [:span.switch-status status]]))

(defn select-template-settings
  "UI component to allow users to select template details, e.g. select a list to be in, lookup value greater than, less than, etc."
  []
  (let [selected-template @(subscribe [:selected-template])
        service @(subscribe [:selected-template-service])
        lists @(subscribe [:current-lists])
        all-constraints (:where selected-template)
        model (assoc (:model service) :type-constraints all-constraints)]
    [:div.col-xs-4.border-right
     (into [:div.form]
           ; Only show editable constraints, but don't filter because we want the index!
           (->> (keep-indexed (fn [idx con] (if (:editable con) [idx con])) all-constraints)
                (map (fn [[idx {:keys [switched switchable] :as con}]]
                       [:div.template-constraint-container
                        [constraint
                         :model model
                         :typeahead? true
                         :path (:path con)
                         :value (or (:value con) (:values con))
                         :op (:op con)
                         :label (s/join " > " (take-last 2 (s/split (im-path/friendly model (:path con)) #" > ")))
                         :code (:code con)
                         :hide-code? true
                         :label? true
                         :disabled (= switched "OFF")
                         :lists lists
                         :on-blur (fn [new-constraint]
                                    (dispatch [:template-chooser/replace-constraint
                                               idx (merge (cond-> con
                                                            (contains? new-constraint :values) (dissoc :value)
                                                            (contains? new-constraint :value) (dissoc :values)) new-constraint)])
                                    (dispatch [:template-chooser/update-preview
                                               idx (merge (cond-> con
                                                            (contains? new-constraint :values) (dissoc :value)
                                                            (contains? new-constraint :value) (dissoc :values)) new-constraint)]))
                         :on-change (fn [new-constraint]
                                      (dispatch [:template-chooser/replace-constraint
                                                 idx (merge (cond-> con
                                                              (contains? new-constraint :values) (dissoc :value)
                                                              (contains? new-constraint :value) (dissoc :values)) new-constraint)]))]
                        (when switchable
                          [toggle {:status switched
                                   :on-change (fn [new-constraint]
                                                (dispatch [:template-chooser/replace-constraint
                                                           idx (assoc con :switched (case switched "ON" "OFF" "ON"))])
                                                (dispatch [:template-chooser/update-preview
                                                           idx (assoc con :switched (case switched "ON" "OFF" "ON"))]))}])]))))]))

(defn tags
  "UI element to visually output all aspect tags into each template card for easy scanning / identification of tags.
  ** Expects: vector of strings 'im:aspect:thetag'."
  [tagvec authorized]
  (let [aspects (for [category (categories-from-tags tagvec)]
                  [:span.tag-type
                   {:class (str "type-" category)
                    :on-click (fn [evt]
                                (.stopPropagation evt)
                                (dispatch [:template-chooser/set-category-filter category]))}
                   category])]
    ;; This element should still be present even when it has no contents.
    ;; The "View >>" button is absolute positioned, so otherwise it would
    ;; overlap with the template's description.
    (into [:div.template-tags
           [:div.permissions
            (when (contains? (set tagvec) "im:public")
              [:span.permission
               [poppable {:data "This is a public template, visible to all users whether logged in or not."
                          :children [icon "globe"]}]])
            (when authorized
              [:span.permission
               [poppable {:data "This template is owned by you."
                          :children [icon "user-circle" nil ["authorized"]]}]])]]

          (when (seq aspects)
            (cons "Categories: " aspects)))))

(defn template
  "UI element for a single template."
  [[id query]]
  (let [title (:title query)
        selected-template-name @(subscribe [:selected-template-name])
        selected? (= id selected-template-name)]
    [:div.grid-1
     [:div.col.ani.template
      {:class (when selected? "selected")
       :id (name id)
       :on-click #(when (not selected?)
                    (dispatch [::route/navigate ::route/template {:template (name id)}]))}
      (into [:h4]
            (if (ascii-arrows title)
              (ascii->svg-arrows title)
              [[:span title]]))
      [:div.description
       {:dangerouslySetInnerHTML {:__html (:description query)}}]
      (when selected?
        [:div.body
         [select-template-settings]
         [preview-results]])
      (if selected?
        [:button.view
         {:on-click #(dispatch [::route/navigate ::route/templates])}
         "Close <<"]
        [:button.view
         "View >>"])
      [tags (:tags query) (:authorized query)]]]))

(defn templates
  "Outputs all the templates that match the user's chosen filters."
  []
  (fn [templates]
    (if (seq templates)
      ;;return the list of templates if there are some
      (into [:div.template-list] (map (fn [t] [template t]) templates))
      ;;if there are no templates, perhaps because of filters or perhaps not...
      [:div.no-results
       [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
       " No templates available"
       (let [category-filter @(subscribe [:selected-template-category])
             text-filter @(subscribe [:template-chooser/text-filter])
             authorized-filter @(subscribe [:template-chooser/authorized-filter])
             filters-active? (or (some? category-filter)
                                 (not (blank? text-filter))
                                 authorized-filter)]
         (cond filters-active?

               [:span
                [:span
                 (cond category-filter
                       (str " in the '" category-filter "' category"))
                 (cond (not-empty text-filter)
                       (str " containing the text '" text-filter "'"))
                 (cond authorized-filter
                       (str " which are owned by you"))
                 "."]
                [:br]
                [:span "Try "
                 [:a {:on-click
                      (fn []
                        (dispatch [:template-chooser/clear-text-filter])
                        (dispatch [:template-chooser/set-category-filter nil])
                        (when authorized-filter
                          (dispatch [:template-chooser/toggle-authorized-filter])))}
                  "removing the filters"]
                 " to view more results. "]]))])))

(defn template-filter []
  (let [input (reagent/atom @(subscribe [:template-chooser/text-filter]))
        debounced (debounce #(dispatch [:template-chooser/set-text-filter %]) 500)
        on-change (fn [e]
                    (let [value (oget e :target :value)]
                      (reset! input value)
                      (debounced value)))]
    (fn []
      [:input.form-control.input-lg
       {:id "template-text-filter"
        :type "text"
        :value @input
        :placeholder "Search for keywords"
        :autoFocus true
        :on-change on-change}])))

(defn authorized-filter []
  (let [filter-authorized? @(subscribe [:template-chooser/authorized-filter])
        authed? @(subscribe [:bluegenes.subs.auth/authenticated?])
        im-version @(subscribe [:current-intermine-version])
        not-compatible? (not (compatible-version? "5.0.4" im-version))]
    (when authed?
      [:button.btn.btn-link.btn-slim
       {:on-click #(dispatch [:template-chooser/toggle-authorized-filter])
        :disabled not-compatible?}
       [poppable {:data (if not-compatible?
                          "This mine is running an older InterMine version which does not support filtering to templates owned by you."
                          "Click to toggle filtering of templates to only those owned by you")
                  :options {:data-placement "bottom"}
                  :children [icon "user-circle" 2 (when filter-authorized? ["authorized"])]}]])))

(defn filters []
  [:div.template-filters
   [:div.template-filter-container.container
    [:div.template-filter.text-filter
     [:label.control-label
      {:for "template-text-filter"}
      "Filter by text"]
     [:div.filter-input
      [template-filter]
      [authorized-filter]]]
    [:div.template-filter
     [:label.control-label "Filter by category"]
     [categories]]]])

(defn main []
  (let [im-templates (subscribe [:templates-by-category])]
    (fn []
      [:div.template-component-container
       [filters]
       [:div.container.template-container
        [:div.row
         [:div.col-xs-12.templates
          [:div.template-list
           [templates @im-templates]]]]]
       [top-scroll/main]])))
