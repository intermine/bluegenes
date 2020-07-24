(ns bluegenes.pages.lists.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.pages.lists.utils :refer [folder?]]
            [cljs-time.format :as time-format]
            [cljs-time.coerce :as time-coerce]))

(defn filter-lists []
  [:div.filter-lists
   [:h2 "Filter lists"]
   [:div.filter-input
    [:input {:type "text"
             :placeholder "Search for keywords"}]
    [icon "search"]]])

(defn controls []
  [:div.controls
   [:button.btn.btn-raised
    {:disabled true}
    "New folder" [icon "new-folder"]]
   [:button.btn.btn-raised
    "Combine lists" [icon "venn-combine"]]
   [:button.btn.btn-raised
    "Intersect lists" [icon "venn-intersection"]]
   [:button.btn.btn-raised
    "Exclude lists" [icon "venn-disjunction"]]
   [:button.btn.btn-raised
    "Subtract lists" [icon "venn-difference"]]])

(def list-time-formatter (time-format/formatter "dd MMM, Y"))

(defn lists []
  (let [filtered-lists @(subscribe [:lists/filtered-lists])
        expanded-paths @(subscribe [:lists/expanded-paths])]
    [:section.lists-table

     [:header.lists-row.lists-headers
      [:div.lists-col
       [:input {:type "checkbox"}]]
      [:div.lists-col
       [:div.list-header
        [:span (str "List details (" "All" ")")]
        [:button.btn [icon "sort"]]
        [:div.dropdown
         [:button.btn.dropdown-toggle
          {:data-toggle "dropdown"}
          [icon "selection"]]
         [:ul.dropdown-menu
          [:li.active [:a "All"]]
          [:li [:a "Private only"]]
          [:li [:a "Public only"]]
          [:li [:a "Folders first"]]]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Date"]
        [:button.btn [icon "sort"]]
        [:div.dropdown
         [:button.btn.dropdown-toggle
          {:data-toggle "dropdown"}
          [icon "selection"]]
         [:ul.dropdown-menu
          [:li.active [:a "All"]]
          [:li [:a "Today"]]
          [:li [:a "Last week"]]
          [:li [:a "Last month"]]
          [:li [:a "Last year"]]]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Type"]
        [:button.btn [icon "sort"]]
        [:button.btn [icon "selection"]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Tags"]
        [:button.btn [icon "sort"]]
        [:button.btn [icon "selection"]]]]
      [:div.lists-col]]

     (doall
      (for [{:keys [id title size authorized description dateCreated type tags
                    path is-last]
             :as item}
            filtered-lists
            :let [is-folder (folder? item)
                  is-expanded (and is-folder (contains? expanded-paths path))]]
        ^{:key id}
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
             [icon "user-circle"]
             [icon "globe"])]
          [:p.list-description description]]

         [:div.lists-col
          (time-format/unparse list-time-formatter
                               (time-coerce/from-string dateCreated))]

         [:div.lists-col
          (when-not is-folder
            [:code.start {:class (str "start-" type)}
             type])]

         (into [:div.lists-col]
               (for [tag tags
                     ;; Hide internal tags.
                     :when (not (str/includes? tag ":"))]
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
           [:button.btn [icon "list-delete"]]]]]))]))

(defn main []
  [:div.container-fluid.lists
   [filter-lists]
   [controls]
   [lists]])
