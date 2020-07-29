(ns bluegenes.pages.lists.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.pages.lists.utils :refer [folder? internal-tag?]]
            [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]
            [oops.core :refer [oget]]
            [goog.functions :refer [debounce]]))

(defn filter-lists []
  (let [input (r/atom @(subscribe [:lists/keywords-filter]))
        debounced (debounce #(dispatch [:lists/set-keywords-filter %]) 500)
        on-change (fn [e]
                    (let [value (oget e :target :value)]
                      (reset! input value)
                      (debounced value)))]
    (fn []
      [:div.filter-lists
       [:h2 "Filter lists"]
       [:div.filter-input
        [:input {:type "text"
                 :placeholder "Search for keywords"
                 :on-change on-change
                 :value @input}]
        [icon "search"]]])))

(defn controls []
  [:div.controls
   [:button.btn.btn-raised
    {:disabled true}
    "Combine lists" [icon "venn-combine"]]
   [:button.btn.btn-raised
    {:disabled true}
    "Intersect lists" [icon "venn-intersection"]]
   [:button.btn.btn-raised
    {:disabled true}
    "Exclude lists" [icon "venn-disjunction"]]
   [:button.btn.btn-raised
    {:disabled true}
    "Subtract lists" [icon "venn-difference"]]])

(def list-time-formatter (time-format/formatter "dd MMM, Y"))
(def list-time-formatter-full (time-format/formatter "dd MMMM, Y"))

(defn parse-date-created [dateCreated & [full-month?]]
  (time-format/unparse (if full-month? list-time-formatter-full list-time-formatter)
                       (time-coerce/from-string dateCreated)))

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

(defn list-row [item]
  (let [{:keys [id title size authorized description dateCreated type tags
                path is-last]} item
        expanded-paths @(subscribe [:lists/expanded-paths])
        is-folder (folder? item)
        is-expanded (and is-folder (contains? expanded-paths path))]
    [:div.lists-row.lists-item
     (when (or is-expanded is-last)
       {:style {:borderBottomWidth 4}})

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
        [:input {:type "checkbox"}]
        [icon "list-item" nil ["list-icon"]]])

     [:div.lists-col
      [:div.list-detail
       [:p.list-title title]
       [:span.list-size (str "[" size "]")]
       (if authorized
         [icon "user-circle" nil ["authorized"]]
         [icon "globe"])]
      [:p.list-description description]]

     [:div.lists-col
      (parse-date-created dateCreated)]

     [:div.lists-col
      (when-not is-folder
        [:code.start {:class (str "start-" type)}
         type])]

     (into [:div.lists-col]
           ;; Hide internal tags.
           (for [tag (remove internal-tag? tags)]
             [:code.tag tag]))

     [:div.lists-col.vertical-align-cell
      [:div.list-controls.hidden-lg
       [:div.dropdown
        [:button.btn.dropdown-toggle
         {:data-toggle "dropdown"}
         [icon "list-more"]]
        [:ul.dropdown-menu.dropdown-menu-right
         [:li [:a "Copy"]]
         [:li [:a "Edit"]]
         [:li [:a "Delete"]]]]]
      [:div.list-controls.hidden-xs.hidden-sm.hidden-md
       [:button.btn [icon "list-copy"]]
       [:button.btn [icon "list-edit"]]
       [:button.btn [icon "list-delete"]]]]]))

(defn lists []
  (let [filtered-lists  @(subscribe [:lists/filtered-lists])
        lists-selection @(subscribe [:lists/filter :lists])
        all-types @(subscribe [:lists/all-types])
        all-tags @(subscribe [:lists/all-tags])]
    [:section.lists-table

     [:header.lists-row.lists-headers
      [:div.lists-col
       [:input {:type "checkbox"}]]
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

(defn main []
  [:div.container-fluid.lists
   [filter-lists]
   [controls]
   [lists]
   [no-lists]])
