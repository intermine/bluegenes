(ns bluegenes.pages.lists.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.pages.lists.utils :refer [folder? internal-tag?]]
            [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]
            [oops.core :refer [oget]]
            [goog.functions :refer [debounce]]
            [bluegenes.components.select-tags :as select-tags]
            [bluegenes.subs.auth :as auth]
            [clojure.string :as str]
            [bluegenes.route :as route]
            [bluegenes.components.bootstrap :refer [poppable]]))

(def set-operations [:combine :intersect :difference :subtract])

(def set-operations->assets
  {:combine {:text "Combine lists"
             :icon [icon "venn-combine"]}
   :intersect {:text "Intersect lists"
               :icon [icon "venn-intersection"]}
   :difference {:text "Difference lists"
                :icon [icon "venn-disjunction"]}
   :subtract {:text "Subtract lists"
              :icon [icon "venn-difference"]}})

(defn find-set-op [f set-op]
  (let [target-index (some (fn [[i op]]
                             (when (= op set-op) (f i)))
                           (map-indexed vector set-operations))]
    (when (contains? set-operations target-index)
      (get set-operations target-index))))

(defn filter-lists []
  (let [input (r/atom @(subscribe [:lists/keywords-filter]))
        debounced (debounce #(dispatch [:lists/set-keywords-filter %]) 500)
        on-change (fn [e]
                    (let [value (oget e :target :value)]
                      (reset! input value)
                      (debounced value)))]
    (fn []
      [:div.filter-lists
       [:h2 "Lists"
        [poppable {:data "When you upload lists, duplicates are removed and the naming of the items standardised. This means that you can do set operations correctly on lists of the same type (e.g. genes), such as finding the intersection between two (or more) lists, and subtracting lists. See the options below - selecting an option provides further guidance and explanation."
                   :children [icon "info"]}]]
       [:div.filter-input
        [:input {:type "text"
                 :placeholder "Search for keywords"
                 :on-change on-change
                 :value @input}]
        [icon "search"]]])))

(defn top-controls []
  (let [insufficient-selected (not @(subscribe [:lists/selected-operation?]))]
    (into [:div.top-controls]
          (for [set-op set-operations]
            [:button.btn.btn-raised
             {:disabled insufficient-selected
              :on-click #(dispatch [:lists/open-modal set-op])}
             (get-in set-operations->assets [set-op :text])
             (get-in set-operations->assets [set-op :icon])]))))

(defn bottom-controls []
  (let [list-count (count @(subscribe [:lists/selected-lists]))]
    (when (pos? list-count)
      [:div.bottom-controls
       [:div
        [:span.selected-indicator
         (str "Selected " list-count (cond-> " list" (> list-count 1) (str "s")))]
        [:button.btn.btn-raised.btn-default
         {:on-click #(dispatch [:lists/clear-selected])}
         "Deselect all"]]
       [:div
        [:button.btn.btn-raised.btn-info
         {:on-click #(dispatch [:lists/open-modal :move])}
         "Move all" [icon "new-folder"]]
        [:button.btn.btn-raised.btn-info
         {:on-click #(dispatch [:lists/open-modal :copy])}
         "Copy all" [icon "list-copy"]]
        [:button.btn.btn-raised.btn-danger
         {:on-click #(dispatch [:lists/open-modal :delete])}
         "Delete all" [icon "list-delete"]]]])))

(defn pagination-items [p pages]
  (concat
   [{:disabled (= p 1) :label "‹" :value (dec p)}]
   (when (> p 1) [{:label 1 :value 1}])
   (when (= p 3) [{:label 2 :value 2}])
   (when (> p 3) [{:label "..."}])
   (for [i (range p (min (inc pages) (+ p 3)))]
     {:active (= i p) :label i :value i})
   (when (< p (- pages 4)) [{:label "..."}])
   (when (= p (- pages 4)) [{:label (dec pages) :value (dec pages)}])
   (when (< p (- pages 2)) [{:label pages :value pages}])
   [{:disabled (= p pages) :label "›" :value (inc p)}]))

(defn pagination []
  (let [per-page @(subscribe [:lists/per-page])
        page-count @(subscribe [:lists/page-count])
        current-page @(subscribe [:lists/current-page])]
    ;; Don't show pagination if there are no pages and therefore no lists.
    (when (pos? page-count)
      ;; This is a guard to switch to the last page if the current one no
      ;; longer exists. The right place to put it would be in :lists/initialize
      ;; but we have to normalize-lists to tell the page count. As a future
      ;; refactoring improvement, you could compute page count as part of
      ;; denormalization and put it in app-db, instead of using it as a sub.
      (when (> current-page page-count)
        (dispatch [:lists/set-current-page page-count]))
      [:div.pagination-controls
       [:span "Rows per page"]
       [:div.dropdown
        [:button.btn.btn-raised.dropdown-toggle.rows-per-page
         {:data-toggle "dropdown"}
         per-page [icon "caret-down"]]
        (into [:ul.dropdown-menu]
              (map (fn [value]
                     [:li {:class (when (= value per-page) :active)}
                      [:a {:on-click #(dispatch [:lists/set-per-page value])}
                       value]])
                   [20 50 100]))]
       (into [:ul.pagination]
             (map (fn [{:keys [disabled active label value]}]
                    [:li {:class (cond disabled :disabled
                                       active :active)}
                     [:a {:disabled disabled
                          :on-click (when (and (not disabled) value)
                                      #(dispatch [:lists/set-current-page value]))}
                      label]])
                  (pagination-items current-page page-count)))])))

(def list-time-formatter (time-format/formatter "dd MMM, Y"))

(defn pretty-time [timestamp]
  (time-format/unparse list-time-formatter
                       (time-coerce/from-long timestamp)))

(defn sort-button [column]
  (let [active-sort @(subscribe [:lists/sort])]
    [:button.btn
     {:on-click #(dispatch [:lists/toggle-sort column])}
     [icon "sort" nil [(when (= column (:column active-sort))
                         (case (:order active-sort)
                           :asc "active-asc-sort"
                           :desc "active-desc-sort"))]]]))

(defn selection-button [filter-name items]
  (let [active-value @(subscribe [:lists/filter filter-name])]
    [:div.dropdown
     [:button.btn.dropdown-toggle
      {:data-toggle "dropdown"}
      [icon "selection" nil [(when (some? active-value)
                               "active-selection")]]]
     (into [:ul.dropdown-menu]
           (for [{:keys [label value]} items]
             [:li {:class (when (= value active-value)
                            "active")}
              [:a {:on-click #(dispatch [:lists/set-filter filter-name value])}
               label]]))]))

(defn list-row-controls [list-id authorized]
  [:<>
   [:button.btn
    {:on-click #(dispatch [:lists/open-modal :copy list-id])}
    [icon "list-copy"]]
   [:button.btn
    {:on-click #(dispatch [:lists/open-modal :edit list-id])
     :disabled (not authorized)}
    [icon "list-edit"]]
   [:button.btn
    {:on-click #(dispatch [:lists/open-modal :delete list-id])
     :disabled (not authorized)}
    [icon "list-delete"]]])

(defn list-row [item]
  (let [{:keys [id title size authorized description timestamp type tags
                path is-last]} item
        expanded-paths @(subscribe [:lists/expanded-paths])
        selected-lists @(subscribe [:lists/selected-lists])
        is-folder (folder? item)
        is-expanded (and is-folder (contains? expanded-paths path))
        is-selected (contains? selected-lists id)]
    [:div.lists-row.lists-item
     {:class (when (or is-expanded is-last) :separator)}

     (if is-folder
       [:div.lists-col
        [:div.list-actions
         (if is-expanded
           [:button.btn
            {:on-click #(dispatch [:lists/collapse-path path])}
            [icon "collapse-folder"]]
           [:button.btn
            {:on-click #(dispatch [:lists/expand-path path])}
            [icon "expand-folder"]])
         (if is-expanded
           [icon "folder-open-item" nil ["list-icon"]]
           [icon "folder-item" nil ["list-icon"]])]]
       [:div.lists-col
        [:input {:type "checkbox"
                 :checked is-selected
                 :on-change #(dispatch [(if (oget % :target :checked)
                                          :lists/select-list
                                          :lists/deselect-list)
                                        id])}]
        [icon "list-item" nil ["list-icon"]]])

     [:div.lists-col
      [:div.list-detail
       (if is-folder
         [:button.btn.btn-link.list-title
          {:on-click (if is-expanded
                       #(dispatch [:lists/collapse-path path])
                       #(dispatch [:lists/expand-path path]))}
          title]
         [:a.list-title
          {:href (route/href ::route/results {:title title})}
          title])
       [:span.list-size (str "[" size "]")]
       (if authorized
         [icon "user-circle" nil ["authorized"]]
         [icon "globe"])]
      [:p.list-description description]]

     [:div.lists-col
      (pretty-time timestamp)]

     [:div.lists-col
      (when-not is-folder
        [:code.start {:class (str "start-" type)}
         type])]

     [:div.lists-col
      (into [:div.list-tags]
            ;; Hide internal tags.
            (for [tag (remove internal-tag? tags)]
              [:code.tag tag]))]

     [:div.lists-col.vertical-align-cell
      (when-not is-folder
        [:<>
         [:div.list-controls.hidden-lg
          [:div.dropdown
           [:button.btn.dropdown-toggle
            {:data-toggle "dropdown"}
            [icon "list-more"]]
           [:div.dropdown-menu.dropdown-menu-controls
            [:div.list-controls
             [list-row-controls id authorized]]]]]
         [:div.list-controls.hidden-xs.hidden-sm.hidden-md
          [list-row-controls id authorized]]])
      (when is-selected
        [:div.selected-list-overlay])]]))

(defn lists []
  (let [filtered-lists @(subscribe [:lists/filtered-lists])
        lists-selection @(subscribe [:lists/filter :lists])
        all-types @(subscribe [:lists/all-types])
        all-tags @(subscribe [:lists/all-tags])
        all-selected? @(subscribe [:lists/all-selected?])]
    [:section.lists-table

     [:header.lists-row.lists-headers
      [:div.lists-col
       [:input {:type "checkbox"
                :checked all-selected?
                :on-change (if all-selected?
                             #(dispatch [:lists/clear-selected])
                             #(dispatch [:lists/select-all-lists]))}]]
      [:div.lists-col
       [:div.list-header
        [:span (str "List details ("
                    (case lists-selection
                      nil "All"
                      :private "Private only"
                      :public "Public only"
                      :folder "Folders first")
                    ")")]
        [sort-button :title]
        [selection-button
         :lists
         [{:label "All" :value nil}
          {:label "Private only" :value :private}
          {:label "Public only" :value :public}
          {:label "Folders first" :value :folder}]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Date"]
        [sort-button :timestamp]
        [selection-button
         :date
         [{:label "All" :value nil}
          {:label "Last day" :value :day}
          {:label "Last week" :value :week}
          {:label "Last month" :value :month}
          {:label "Last year" :value :year}]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Type"]
        [sort-button :type]
        [selection-button
         :type
         (cons {:label "All" :value nil}
               (map (fn [type] {:label type :value type}) all-types))]]]
      [:div.lists-col
       [:div.list-header
        [:span "Tags"]
        [sort-button :tags]
        [selection-button
         :tags
         (cons {:label "All" :value nil}
               (map (fn [tag] {:label tag :value tag}) all-tags))]]]
      [:div.lists-col]]

     (for [{:keys [id] :as item} filtered-lists]
       ^{:key id}
       [list-row item])]))

(defn no-lists []
  (let [no-lists? @(subscribe [:lists/no-lists?])
        no-filtered-lists? @(subscribe [:lists/no-filtered-lists?])
        is-empty (or no-lists? no-filtered-lists?)
        mine-name @(subscribe [:current-mine-human-name])]
    (when is-empty
      [:div.no-lists
       (cond
         no-lists? [:h3 (str mine-name " has no public lists available")]
         no-filtered-lists? [:h3 "No list matches active filters"])
       [:hr]
       [:p "You may have lists saved to your account. Login to access them."]])))

(defn modal-list-row [item & {:keys [subop single?]}]
  (let [{:keys [id title timestamp type tags]} item]
    [:tr
     [:td.title title]
     [:td (pretty-time timestamp)]
     [:td
      [:code.start {:class (str "start-" type)}
       type]]
     [:td
      (into [:div.tags]
            (for [tag (remove internal-tag? tags)]
              [:code.tag tag]))]
     [:td
      (case subop
        :down [:button.btn.pull-right
               {:type "button"
                :on-click #(dispatch [:lists-modal/keep-list id])}
               [icon "move-down-list" 2]]
        :up   [:button.btn.pull-right
               {:type "button"
                :on-click #(dispatch [:lists-modal/subtract-list id])}
               [icon "move-up-list" 2]]
        nil)
      (when-not single?
        [:button.btn.pull-right
         {:type "button"
          :on-click #(dispatch [:lists/deselect-list id])}
         [icon "remove-list" 2]])]]))

(defn modal-table [items & {:keys [subop single?]}]
  [:table.table.table-hover
   [:thead
    [:tr
     [:th "Title"]
     [:th "Date"]
     [:th "Type"]
     [:th "Tags"]
     [:th]]] ; Empty header for row buttons.
   [:tbody
    (for [{:keys [id] :as item} items]
      ^{:key id}
      [modal-list-row item :subop subop :single? single?])]])

(defn modal-new-list [& {:keys [edit-list?]}]
  (let [all-tags @(subscribe [:lists/all-tags])
        new-list-title @(subscribe [:lists-modal/new-list-title])
        new-list-tags @(subscribe [:lists-modal/new-list-tags])
        new-list-description @(subscribe [:lists-modal/new-list-description])
        list-tags-support? @(subscribe [:list-tags-support?])
        logged-in? @(subscribe [::auth/authenticated?])]
    [:<>
     (when-not edit-list?
       [:p "New list"])
     [:div.list-title-tags
      [:div.title-input-container
       [:label {:for "modal-new-list-title"}
        "Title"]
       [:input.form-control
        {:type "text"
         :id "modal-new-list-title"
         :on-change #(dispatch [:lists-modal/set-new-list-title
                                (oget % :target :value)])
         :value new-list-title}]]
      [:div
       [:label {:for "modal-new-list-tags"}
        (str "Tags" (when-not edit-list? " (optional)"))]
       [select-tags/main
        :disabled (or (not list-tags-support?) (not logged-in?))
        :id "modal-new-list-tags"
        :on-change #(dispatch [:lists-modal/set-new-list-tags %])
        :value new-list-tags
        :options all-tags]]]
     [:div.list-description
      [:label {:for "modal-new-list-description"}
       (str "Description" (when-not edit-list? " (optional)"))]
      [:textarea.form-control
       {:id "modal-new-list-description"
        :rows 2
        :on-change #(dispatch [:lists-modal/set-new-list-description
                               (oget % :target :value)])
        :value new-list-description}]]]))

(defn modal-set-operation []
  (let [active-modal @(subscribe [:lists/active-modal])]
    [:<>
     (case active-modal
       (:combine :intersect :difference)
       [:div
        (case active-modal
          :combine [:p "The new list will contain " [:em "all items"] " from the following lists"]
          :intersect [:p "The new list will contain only " [:em "items common"] " to all the following lists"]
          :difference [:p "The new list will contain only " [:em "items unique"] " to each of the following lists"
                       (when-let [selected-lists (not-empty @(subscribe [:lists/selected-lists]))]
                         (when (> (count selected-lists) 2)
                           [:a.notice {:href "https://en.wikipedia.org/wiki/Symmetric_difference" :target "_blank"}
                            " (this will perform a mathematical symmetric difference, which means members of an odd amount of lists will be kept)"
                            [icon "external"]]))])
        [modal-table @(subscribe [:lists/selected-lists-details])]]

       (:subtract)
       [:div.subtract-container
        [:p "The items from these lists"]
        [:div.table-container
         [modal-table @(subscribe [:lists-modal/subtract-lists-details]) :subop :down]]
        [:p "will be removed from these lists to give the new list."]
        [:div.table-container
         [modal-table @(subscribe [:lists-modal/keep-lists-details]) :subop :up]]])

     [modal-new-list]]))

(defn valid-folder?
  "Whether a folder name is valid. Full stops are technically permitted but we
  use them for nesting."
  [s]
  (re-matches #"[A-Za-z0-9 \-:]+" s))

(def invalid-folder-message
  "Folder names may only contain letters, numbers, spaces, hyphens and colons.")

(defn modal-select-folder []
  (let [folder-path @(subscribe [:lists-modal/folder-path])
        folder-suggestions @(subscribe [:lists-modal/folder-suggestions])
        list-tags-support? @(subscribe [:list-tags-support?])
        logged-in? @(subscribe [::auth/authenticated?])
        disable-tags? (or (not list-tags-support?) (not logged-in?))]
    [:div.select-folder
     [icon "modal-folder" 2]
     [:span.folder-path (str/join " / " (conj folder-path ""))]
     [:> js/Select.Creatable
      {:className "folder-selector"
       :placeholder "Choose folder"
       :isValidNewOption #(valid-folder? %1)
       :formatCreateLabel #(str "Create new \"" % "\" folder")
       :noOptionsMessage #(if ((some-fn empty? valid-folder?) (oget % :inputValue))
                            "No existing folders"
                            invalid-folder-message)
       :onChange #(dispatch [:lists-modal/nest-folder (oget % :value)])
       :value nil ; Required or else it will keep its own state.
       :options (map (fn [v] {:value v :label v}) folder-suggestions)
       :isDisabled disable-tags?}]
     [:button.btn.button-folder-up
      {:disabled (or (empty? folder-path) disable-tags?)
       :on-click #(dispatch [:lists-modal/denest-folder])}
      [icon "modal-folder-up" nil ["folder-up"]]]]))

(defn modal-other-operation []
  (let [active-modal @(subscribe [:lists/active-modal])
        selected-lists @(subscribe [:lists/selected-lists-details])
        target-list @(subscribe [:lists-modal/target-list])
        list-items (if target-list [target-list] selected-lists)]
    [:<>
     (case active-modal
       :delete
       [modal-table list-items :single? (boolean target-list)]
       :edit
       [:div
        [modal-new-list :edit-list? true]
        [:p "Current folder"]
        [modal-select-folder]]
       (:copy :move)
       [:div
        [:div.table-container
         [modal-table list-items :single? (boolean target-list)]]
        [:p (case active-modal
              :copy "Copy to folder"
              :move "Move to folder")]
        [modal-select-folder]])]))

(defn modal []
  (let [modal-open? @(subscribe [:lists/modal-open?])
        active-modal @(subscribe [:lists/active-modal])
        error-message @(subscribe [:lists-modal/error])
        list-tags-support? @(subscribe [:list-tags-support?])
        logged-in? @(subscribe [::auth/authenticated?])]
    [:<>
     [:div.fade.modal-backdrop
      {:class (when modal-open? :show)}]
     [:div.modal.fade.show
      {:class (when modal-open? :in)
       :tab-index "-1"
       :role "dialog"}
      [:div.modal-dialog.modal-lg
       [:div.modal-content
        [:div.modal-header
         [:button.close
          {:aria-hidden "true"
           :type "button"
           :on-click #(dispatch [:lists/close-modal])}
          "×"]
         [:h3.modal-title.text-center
          (case active-modal
            (:combine :intersect :difference :subtract)
            [:button.btn.btn-slim.change-set-operation
             (if-let [prev-set-op (find-set-op dec active-modal)]
               {:on-click #(dispatch [:lists/open-modal prev-set-op])}
               {:disabled true})
             [icon "chevron-left"]]
            [:span])
          (case active-modal
            (:combine :intersect :difference :subtract)
            [:span
             (get-in set-operations->assets [active-modal :text])
             (get-in set-operations->assets [active-modal :icon])]
            :delete "Delete list(s)"
            :edit "Edit list"
            :copy "Copy list(s)"
            :move "Move list(s)"
            nil)
          (case active-modal
            (:combine :intersect :difference :subtract)
            [:button.btn.btn-slim.change-set-operation
             (if-let [next-set-op (find-set-op inc active-modal)]
               {:on-click #(dispatch [:lists/open-modal next-set-op])}
               {:disabled true})
             [icon "chevron-right"]]
            [:span])]]

        [:div.modal-body
         (case active-modal
           (:combine :intersect :difference :subtract)
           [modal-set-operation]
           (:delete :edit :copy :move)
           [modal-other-operation]
           nil)

         (cond
           (not-empty error-message)
           [:div.alert.alert-danger
            [:strong error-message]]

           (and (not list-tags-support?) (not= active-modal :delete))
           [:div.alert.alert-inverse
            [:strong "This InterMine is running an older version which does not support adding tags or creating folders"]]

           (and (not logged-in?) (not= active-modal :delete))
           [:div.alert.alert-inverse
            [:strong "You need to login to edit tags or create folders"]])]

        [:div.modal-footer
         [:div.btn-toolbar.pull-right
          [:button.btn.btn-default
           {:type "button"
            :on-click #(dispatch [:lists/close-modal])}
           "Cancel"]
          [:button.btn.btn-primary.btn-raised
           {:type "button"
            :on-click
            (case active-modal
              (:combine :intersect :difference :subtract)
              #(dispatch [:lists/set-operation active-modal])
              :delete
              #(dispatch [:lists/delete-lists])
              :edit
              #(dispatch [:lists/edit-list])
              :copy
              #(dispatch [:lists/copy-lists])
              :move
              #(dispatch [:lists/move-lists])
              #())}
           (case active-modal
             (:combine :intersect :difference :subtract) "Save new list"
             :delete "Delete list(s)"
             :edit "Save"
             :copy "Copy list(s)"
             :move "Move list(s)"
             nil)]]]]]]]))

(defn main []
  [:div.container-fluid.lists
   [filter-lists]
   [top-controls]
   [pagination]
   [lists]
   [no-lists]
   [bottom-controls]
   [modal]])
