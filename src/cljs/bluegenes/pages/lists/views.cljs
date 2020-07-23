(ns bluegenes.pages.lists.views
  (:require [re-frame.core :refer [subscribe]]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]
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
   [:button "New folder"]
   [:button "Combine lists"]
   [:button "Intersect lists"]
   [:button "Exclude lists"]
   [:button "Subtract lists"]])

(comment
  (require '[re-frame.core :refer [subscribe]])
  (first @(subscribe [:lists/filtered-lists])))

(def list-time-formatter (time-format/formatter "dd MMM, Y"))

(defn lists []
  (let [filtered-lists @(subscribe [:lists/filtered-lists])]
    [:section.lists-table

     [:header.lists-row.lists-headers
      [:div.lists-col
       [:input {:type "checkbox"}]]
      [:div.lists-col
       [:div.list-header
        [:span "List details"]
        [:button.btn [icon "sort"]]
        [:button.btn [icon "selection"]]]]
      [:div.lists-col
       [:div.list-header
        [:span "Date"]
        [:button.btn [icon "sort"]]
        [:button.btn [icon "selection"]]]]
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

     (for [{:keys [id title description dateCreated type tags]} filtered-lists]
       ^{:key id}
       [:div.lists-row.lists-item
        [:div.lists-col
         [:input {:type "checkbox"}]
         [icon "list-item" nil ["list-icon"]]]
        [:div.lists-col
         [:p.list-title title]
         [:p.list-description description]]
        [:div.lists-col
         (time-format/unparse list-time-formatter
                              (time-coerce/from-string dateCreated))]
        [:div.lists-col
         [:code.start {:class (str "start-" type)}
          type]]
        (into [:div.lists-col]
              (for [tag tags]
                [:code.tag tag]))
        [:div.lists-col.vertical-align-cell
         [:div.list-controls
          [:button.btn [icon "list-copy"]]
          [:button.btn [icon "list-edit"]]
          [:button.btn [icon "list-delete"]]]]])]))

(defn main []
  [:div.container-fluid.lists
   [filter-lists]
   [controls]
   [lists]])
