(ns bluegenes.pages.admin.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.pages.admin.events :as events]
            [bluegenes.pages.admin.subs :as subs]
            [bluegenes.components.icons :refer [icon]]
            [oops.core :refer [oget ocall]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [md-element compatible-version?]]
            [bluegenes.route :as route]
            [bluegenes.components.select-tags :as select-tags]))

(defn on-enter [f]
  (fn [e]
    (when (= 13 (oget e :charCode))
      (f e))))

(defn report-layout-dropdown []
  (let [categorize-class (subscribe [::subs/categorize-class])
        options (subscribe [::subs/categorize-options])]
    (when (nil? @categorize-class)
      (dispatch [::events/set-categorize-class (->> @options first key name)]))
    (fn []
      [:div.input-group.categories-dropdown
       [:label.control-label
        {:for "admin__report-category"}
        "Manage layout"]
       (into [:select.form-control
              {:id "admin__report-category"
               :on-change #(dispatch [::events/set-categorize-class (oget % :target :value)])
               :value (or @categorize-class "")}]
             (map (fn [[class-kw details :as item]]
                    (if (map-entry? item)
                      [:option {:value (name class-kw)} (:displayName details)]
                      [:option {:disabled true :role "separator"} "─────────────────────────"]))
                  @options))])))

(defn report-layout-child [_ {:keys [description]}]
  (let [edit-description* (reagent/atom false)
        description* (reagent/atom (or description ""))
        show-preview* (reagent/atom false)]
    (fn [[category-index child-index] {:keys [label type collapse description]}]
      (let [dispatch-idx (fn [[evt & args]]
                           (dispatch (into [evt category-index child-index] args)))]
        [:<>
         [:li.class-entry
          [:div
           [:span label]
           [:span.type type]]
          [:div.btn-group-sm.class-options
           [:button.btn.btn-default.btn-fab
            {:on-click (fn [_evt]
                         (swap! edit-description* not)
                         (reset! description* description))}
            [poppable {:data [:span (if (seq description)
                                      "Edit the description that can be viewed from header"
                                      "Add a description that can be viewed from header")]
                       :children [icon "info" 2 (when (seq description) [:enabled-icon])]}]]
           [:button.btn.btn-default.btn-fab
            {:on-click #(dispatch-idx [::events/child-set-collapse (not collapse)])}
            [poppable {:data [:span (if collapse
                                      "Header will be visible but content needs to be toggled to show"
                                      "Header and content will be visible")]
                       :children [icon (if collapse "eye-blocked" "eye") 2 (when-not collapse [:enabled-icon])]}]]
           [:button.btn.btn-default.btn-fab
            {:on-click #(dispatch-idx [::events/child-move-up])}
            [icon "move-up-list" 2]]
           [:button.btn.btn-default.btn-fab
            {:on-click #(dispatch-idx [::events/child-move-down])}
            [icon "move-down-list" 2]]
           [:button.btn.btn-default.btn-fab
            {:on-click #(dispatch-idx [::events/child-remove])}
            [icon "remove-list" 2]]]]
         (when @edit-description*
           [:div
            (when @show-preview*
              [md-element @description*])
            [:textarea.form-control
             {:rows 2
              :autoFocus true
              :placeholder (str "Describe the contents of this " type " (markdown supported).")
              :value @description*
              :on-change #(reset! description* (oget % :target :value))}]
            [:div.btn-group.btn-group-lg
             [:button.btn.btn-primary.btn-raised.btn-sm
              {:on-click (fn [_evt]
                           (dispatch-idx [::events/child-set-description @description*])
                           (reset! edit-description* false))}
              "Save"]
             [:button.btn.btn-default.btn-raised.btn-sm
              {:on-click (fn [_evt]
                           (reset! edit-description* false)
                           (reset! description* description))}
              "Cancel"]
             [:button.btn.btn-warning.btn-raised.btn-sm
              {:on-click (fn [_evt]
                           (dispatch-idx [::events/child-set-description nil])
                           (reset! description* "")
                           (reset! edit-description* false))}
              "Clear"]
             [:label.description-preview-checkbox
              [:input
               {:type "checkbox"
                :checked @show-preview*
                :on-change #(swap! show-preview* not)}]
              "Show preview"]]])]))))

(defn report-layout-category []
  (let [cat-class (subscribe [::subs/categorize-class])
        available-classes (subscribe [::subs/available-class-names @cat-class])
        available-tools (subscribe [::subs/available-tool-names @cat-class])
        available-templates (subscribe [::subs/available-template-names @cat-class])
        active* (reagent/atom false)
        renaming* (reagent/atom false)
        rename-ref* (reagent/atom nil)
        select-ref* (reagent/atom nil)]
    (fn [category-index {:keys [category children]}]
      [:li {:class (when @active* :active)}
       (if @renaming*
         [:a.rename-category.input-group-sm
          {:on-click #(swap! active* not)}
          [:input.form-control.input-sm
           {:ref #(when %
                    (reset! rename-ref* %)
                    (.focus %))
            :placeholder category
            :on-key-press (on-enter (fn [e]
                                      (when-let [value (not-empty (oget e :target :value))]
                                        (dispatch [::events/category-rename category-index value]))
                                      (reset! renaming* false)))
            :on-click #(.stopPropagation %)
            :default-value category}]
          [:button.btn.btn-default.btn-raised.btn-xs
           {:on-click (fn [e]
                        (.stopPropagation e)
                        (when-let [rename-elem @rename-ref*]
                          (when-let [value (not-empty (oget rename-elem :value))]
                            (dispatch [::events/category-rename category-index value]))
                          (reset! renaming* false)))}
           "Save"]
          [:button.btn.btn-default.btn-raised.btn-xs
           {:on-click (fn [e]
                        (.stopPropagation e)
                        (reset! renaming* false))}
           "Cancel"]]
         [:a {:on-click #(swap! active* not)}
          category
          [:div
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click (fn [e]
                         (.stopPropagation e)
                         (reset! renaming* true))}
            "Rename"]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click (fn [e]
                         (.stopPropagation e)
                         (dispatch [::events/category-move-up category-index]))}
            "Move up"]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click (fn [e]
                         (.stopPropagation e)
                         (dispatch [::events/category-move-down category-index]))}
            "Move down"]
           [:button.btn.btn-danger.btn-raised.btn-xs
            {:on-click (fn [e]
                         (.stopPropagation e)
                         (dispatch [::events/category-remove category-index]))}
            "Delete"]]])
       (when @active*
         [:ul.classes
          (concat
           (for [[i child] (map-indexed vector children)]
             ^{:key (:id child)}
             [report-layout-child [category-index i] child])
           [[:li.add-child
             {:key "addchild"}
             [:div.full-width
              [:> js/Select
               {:placeholder "Available classes, tools and templates"
                :isMulti true
                :ref #(when % (reset! select-ref* %))
                :options [{:label "Classes"
                           :options @available-classes}
                          {:label "Tools"
                           :options @available-tools}
                          {:label "Templates"
                           :options @available-templates}]}]]
             [:div.btn-group-sm
              [:button.btn.btn-default.btn-fab
               {:on-click #(when-let [select-elem @select-ref*]
                             (when-let [children (seq (js->clj (oget select-elem :state :value) :keywordize-keys true))]
                               (dispatch [::events/children-add category-index children])
                               (ocall (oget select-elem :select) :clearValue)))}
               [icon "plus"]]]]])])])))

(defn report-layout-selector []
  (let [categories (subscribe [::subs/categories])
        new-category (subscribe [::subs/new-category])
        input-ref* (reagent/atom nil)]
    (fn []
      [:div.categories-selector
       (when (seq @categories)
         [:em "Click on a category to expand/collapse its children"])
       [:ul.nav.nav-pills.nav-stacked
        (concat
         (for [[i category] (map-indexed vector @categories)]
           ^{:key (:id category)}
           [report-layout-category i category])
         [[:li.add-category.input-group-sm
           {:key "addcat"}
           [:input.form-control.input-sm
            {:ref #(when % (reset! input-ref* %))
             :placeholder "New category"
             :on-change #(dispatch [::events/set-new-category (oget % :target :value)])
             :on-key-press (on-enter
                            #(do (dispatch [::events/category-add @new-category])
                                 (some-> @input-ref* .focus)))
             :value @new-category}]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click #(do (dispatch [::events/category-add @new-category])
                            (some-> @input-ref* .focus))
             :disabled (empty? @new-category)}
            "Add category"]]])]])))

(defn runnable-templates-tooltip []
  [poppable {:data [:div
                    [:p "Some templates can automatically be run on a report page if they meet certain conditions."]
                    [:ol
                     [:li "They must have a single editable constraint"]
                     [:li "That single constraint must be of type LOOKUP"]
                     [:li "That single constraint must be backed by the same class as the item on the report page"]]]
             :children [icon "info"]}])

(defn report-layout []
  (let [bg-properties-support? @(subscribe [:bg-properties-support?])
        response @(subscribe [::subs/responses :report-layout])
        dirty? @(subscribe [::subs/dirty?])]
    [:div.well.well-lg
     [:h3 "Report page layout"]
     [:p "Create categories containing classes (references/collections in the model), tools (visualizations) and/or runnable templates " [runnable-templates-tooltip] ", to be used to determine the layout of report pages. Each layout applies to all report pages for a specific " [:strong "class"] " (e.g. " [:em "Gene"] "). If a class doesn't have a layout created for it, one will be autogenerated when opening the report page."]
     [report-layout-dropdown]
     [report-layout-selector]
     (when (not bg-properties-support?)
       [:div.alert.alert-warning
        [:strong "This InterMine is running an older version which does not support saving BlueGenes layouts. You will still be able to create layouts for testing, but they will disappear when refreshing or closing the browser tab. The layouts you create will also not be available to other users."]])
     [:div.flex-row
      [:button.btn.btn-primary.btn-raised
       {:on-click #(dispatch [::events/save-layout bg-properties-support?])}
       "Save changes"]
      (when-let [{:keys [type message]} (if dirty?
                                          {:type "failure"
                                           :message "You have unsaved changes!"}
                                          response)]
        [:p {:class type} message])]]))

(defn set-notice []
  (let [notice @(subscribe [:current-mine/notice])
        notice-text* (reagent/atom (or notice ""))]
    (fn []
      (let [bg-properties-support? @(subscribe [:bg-properties-support?])
            response @(subscribe [::subs/responses :notice])]
        [:div.well.well-lg.set-notice
         [:h3 "Set homepage notice"]
         [:p "This notice will be displayed at the top of the homepage for all BlueGenes instances that connect to this mine. Useful for communicating scheduled downtime. Markdown is supported, so you'll probably want to prepend with " [:em "###"] " for a decent header size."]
         [:textarea.form-control
          {:rows 4
           :disabled (not bg-properties-support?)
           :value @notice-text*
           :on-change #(reset! notice-text* (oget % :target :value))
           :on-focus #(dispatch [::events/clear-notice-response])}]
         (when (seq @notice-text*)
           [:div.well.well-sm.well-warning.text-center.notice-preview
            [md-element @notice-text*]])
         (when (not bg-properties-support?)
           [:div.alert.alert-warning
            [:strong "This InterMine is running an older version which does not support setting a homepage notice."]])
         [:div.flex-row
          [:button.btn.btn-primary.btn-raised
           {:on-click #(dispatch [::events/save-notice @notice-text*])
            :disabled (not bg-properties-support?)}
           "Save changes"]
          [:button.btn.btn-default.btn-raised
           {:on-click (fn []
                        (reset! notice-text* "")
                        (dispatch [::events/save-notice nil]))
            :disabled (not bg-properties-support?)}
           "Clear notice"]
          (when-let [{:keys [type message]} response]
            [:p {:class type} message])]]))))

(defn template-checkbox [template-name]
  (let [checked? @(subscribe [::subs/template-checked? template-name])]
    [:td [:input
          {:type "checkbox"
           :checked checked?
           :on-click #(dispatch [(if checked?
                                   ::events/uncheck-template
                                   ::events/check-template) template-name])}]]))

(defn template-tags [{:keys [tags]}]
  (let [new-tags* (reagent/atom tags)
        all-tags (subscribe [::subs/all-template-tags])]
    (fn [{:keys [tags editing-tags? template-name]}
         & {:keys [preview?]}]
      (if @editing-tags?
        [:td.tags-cell.is-editing
         [:div.select-tags-container
          [select-tags/main
           :options @all-tags
           :on-change #(reset! new-tags* %)
           :value @new-tags*]]
         [:div.btn-group
          [:button.btn.btn-link.edit-tags-button
           {:on-click (fn []
                        (swap! editing-tags? not)
                        (dispatch [::events/update-template-tags template-name tags @new-tags*]))}
           [icon "checkmark"]]
          [:button.btn.btn-link.edit-tags-button.edit-tags-button-cancel
           {:on-click (fn []
                        (reset! new-tags* tags)
                        (swap! editing-tags? not))}
           [icon "close"]]]]
        (-> [:td.tags-cell]
            (into (for [tag tags]
                    [:code.start tag]))
            (cond->
              (not preview?)
              (conj [:button.btn.btn-link.edit-tags-button
                     {:on-click #(swap! editing-tags? not)}
                     [icon "edit"]])))))))

(defn template []
  (let [editing-tags? (reagent/atom false)]
    (fn [{:keys [name title description comment rank
                 tags]}
         & {:keys [preview?]}]
      (let [precomputed-status (get @(subscribe [::subs/precomputes]) (keyword name))
            summarised-status (get @(subscribe [::subs/summarises]) (keyword name))]
        [:tr
         (when-not preview? [template-checkbox name])
         [:td.name-cell name]
         [:td title]
         [:td.description-cell description]
         [:td.comment-cell comment]
         [:td rank]
         [template-tags
          {:tags tags
           :editing-tags? editing-tags?
           :template-name name}
          :preview? preview?]
         (cond
           preview? nil
           @editing-tags? [:td.empty-cell]
           :else
           [:td
            [:a {:href (route/href ::route/template {:template name})}
             "View"]
            " | "
            [:a {:role "button"
                 :on-click #(dispatch [::events/export-templates-modal [name]])}
             "Export"]
            " | "
            (case precomputed-status
              true "Precomputed"
              :in-progress "Precomputing..."
              [:a {:role "button"
                   :on-click #(dispatch [::events/precompute-template name])}
               "Precompute"])
            " | "
            (case summarised-status
              true "Summarised"
              :in-progress "Summarising..."
              [:a {:role "button"
                   :on-click #(dispatch [::events/summarise-template name])}
               "Summarise"])])]))))

(defn template-filter []
  (let [value @(subscribe [::subs/template-filter])]
    [:div
     [:input.form-control
      {:type "text"
       :placeholder "Filter templates"
       :on-change #(dispatch [::events/set-template-filter (oget % :target :value)])
       :value value}]]))

(defn templates-table [filtered-templates authorized-templates
                       & {:keys [preview?]}]
  [:div.table-container
   [:table.table.templates-table
    [:thead
     [:tr
      (when-not preview?
        (let [all-checked? @(subscribe [::subs/all-templates-checked?])]
          [:th [:input
                {:type "checkbox"
                 :checked all-checked?
                 :on-click #(dispatch [(if all-checked?
                                         ::events/uncheck-all-templates
                                         ::events/check-all-templates)
                                       authorized-templates])}]]))
      [:th "Name"]
      [:th "Title"]
      [:th "Description"]
      [:th "Comment"]
      [:th.rank-header "Rank"]
      [:th "Tags"]
      (when-not preview?
        [:th "Actions"])]]
    [:tbody
     (for [template-details filtered-templates]
       ^{:key (:name template-details)}
       [template template-details
        :preview? preview?])]]])

(defn manage-templates []
  (let [filtered-templates @(subscribe [::subs/filtered-templates])
        authorized-templates @(subscribe [::subs/authorized-templates])
        text-filter @(subscribe [::subs/template-filter])
        checked-templates @(subscribe [::subs/checked-templates])
        im-version @(subscribe [:current-intermine-version])
        not-compatible? (not (compatible-version? "5.0.4" im-version))]
    [:div.well.well-lg.manage-templates
     [:h3 "Manage templates"
      [template-filter]]

     (cond
       not-compatible? [:div.alert.alert-warning
                        [:p "This mine is running an older InterMine version which does not support managing templates."]
                        [:p "Please ask the mine admin to upgrade to 5.0.4 or above."]]
       (and (empty? filtered-templates)
            (seq authorized-templates)) [:p (str "No templates matching filter: " text-filter)]
       (empty? filtered-templates) [:p "Templates you create will appear here."]
       :else [templates-table filtered-templates authorized-templates])

     [:div.btn-group
      [:button.btn.btn-default.btn-raised
       {:disabled (empty? checked-templates)
        :on-click #(dispatch [::events/uncheck-all-templates])}
       "Clear"]
      [:button.btn.btn-danger.btn-raised
       {:disabled (empty? checked-templates)
        :on-click #(dispatch [::events/delete-templates-modal checked-templates])}
       "Delete"]
      [:button.btn.btn-default.btn-raised
       {:disabled (empty? checked-templates)
        :on-click #(dispatch [::events/export-templates-modal checked-templates])}
       "Export"]]]))

(defn import-templates []
  (let [input (reagent/atom "")]
    (fn []
      [:div.well.well-lg
       [:h3 "Import templates"]
       [:p "Paste a template query (or multiple) in XML format below and press " [:strong "submit"] " to load the template(s) into your account."]
       [:textarea.form-control
        {:rows 10
         :value @input
         :on-change #(reset! input (oget % :target :value))}]
       [:div.btn-group
        [:button.btn.btn-raised
         {:on-click (fn []
                      (when-let [template-xml @input]
                        (dispatch [::events/import-template template-xml]))
                      (reset! input ""))}
         "Submit"]]])))

(defn modal []
  (let [clipboard (atom nil)
        msg (reagent/atom nil)]
    (fn []
      (let [{:keys [type xml templates] :as modal-data} @(subscribe [::subs/modal])
            close-modal! (fn []
                           (dispatch [::events/clear-modal])
                           (reset! msg nil))]
        [:<>
         [:div.fade.modal-backdrop
          {:class (when modal-data :show)}]
         [:div.modal.fade.show
          {:class (when modal-data :in)
           :tab-index "-1"
           :role "dialog"}
          [:div.modal-dialog.modal-lg
           [:div.modal-content

            [:div.modal-header
             [:button.close
              {:aria-hidden "true"
               :type "button"
               :on-click close-modal!}
              "×"]
             [:h3.modal-title.text-center
              (case type
                :export "Template Query XML"
                :delete "Delete templates"
                nil)]]

            (case type

              :export
              [:div.modal-body
               [:textarea.hidden-clipboard
                {:ref #(reset! clipboard %)
                 :value xml
                 :read-only true}]
               [:pre xml]]

              :delete
              [:div.modal-body
               [:p "Are you sure you want to delete the following "
                [:strong (count templates)]
                " templates?"]
               [templates-table templates nil :preview? true]]

              nil)

            [:div.modal-footer
             [:div.pull-left
              (when-let [{:keys [type text]} @msg]
                [:p {:class type} text])]
             (case type

               :export
               [:div.btn-toolbar.pull-right
                [:button.btn.btn-default
                 {:on-click close-modal!}
                 "Close"]
                [:button.btn.btn-primary.btn-raised
                 {:on-click #(when-let [clip @clipboard]
                               (.focus clip)
                               (.select clip)
                               (try
                                 (ocall js/document :execCommand "copy")
                                 (reset! msg {:type "success" :text "Copied to clipboard"})
                                 (catch js/Error _
                                   (reset! msg {:type "failure" :text "Failed to copy to clipboard"}))))}
                 "Copy XML"]]

               :delete
               [:div.btn-toolbar.pull-right
                [:button.btn.btn-default
                 {:on-click close-modal!}
                 "Cancel"]
                [:button.btn.btn-danger.btn-raised
                 {:on-click (fn []
                              (dispatch [::events/delete-templates templates])
                              (close-modal!))}
                 "Delete templates"]]

               nil)]]]]]))))

;; These use a qualified keyword as they're used several places. Make sure to
;; check for references before renaming them/adding new ones.
(def pills
  [{:label "Report"
    :value :admin.pill/report}
   {:label "Home"
    :value :admin.pill/home}
   {:label "Templates"
    :value :admin.pill/template}])

(defn main []
  (let [active-pill @(subscribe [::subs/active-pill])]
    [:div.admin-page.container
     (into [:ul.nav.nav-pills]
           (for [{:keys [label value]} pills]
             [:li
              {:class (when (= active-pill value) :active)}
              [:a {:role "button"
                   :on-click #(dispatch [::events/set-active-pill value])}
               label]]))
     (case active-pill
       :admin.pill/report [report-layout]
       :admin.pill/home [set-notice]
       :admin.pill/template [:<>
                             [manage-templates]
                             [import-templates]]
       nil)
     [modal]]))
