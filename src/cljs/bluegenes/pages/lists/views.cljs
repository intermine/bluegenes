(ns bluegenes.pages.lists.views
  (:require [re-frame.core :refer [subscribe]]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]))

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

(defn lists []
  (let [filtered-lists @(subscribe [:lists/filtered-lists])]
    [:section.lists-table

     [:header.lists-row.lists-headers
      [:div.lists-col
       [:input {:type "checkbox"}]
       [:button "All" [icon "selection"]]]
      [:div.lists-col
       [:span "List details"]
       [:button [icon "sort"]]
       [:button [icon "selection"]]]
      [:div.lists-col
       [:span "Date"]
       [:button [icon "sort"]]
       [:button [icon "selection"]]]
      [:div.lists-col
       [:span "Type"]
       [:button [icon "sort"]]
       [:button [icon "selection"]]]
      [:div.lists-col
       [:span "Tags"]
       [:button [icon "sort"]]
       [:button [icon "selection"]]]
      [:div.lists-col
       [:span "More"]]]

     (for [{:keys [id title description dateCreated type tags]} filtered-lists]
       ^{:key id}
       [:div.lists-row.lists-item
        [:div.lists-col
         [:input {:type "checkbox"}]
         [icon "list-item"]]
        [:div.lists-col
         [:p.list-title title]
         [:p.list-description description]]
        [:div.lists-col dateCreated]
        [:div.lists-col type]
        [:div.lists-col (str/join ", " tags)]
        [:div.lists-col id]])]))

(defn main []
  [:div.container-fluid.lists
   [filter-lists]
   [controls]
   [lists]])
