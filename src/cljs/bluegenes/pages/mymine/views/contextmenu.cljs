(ns bluegenes.pages.mymine.views.contextmenu
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.pages.mymine.events :as evts]
            [bluegenes.pages.mymine.subs :as subs]
            [oops.core :refer [ocall]]))

(defn tag-context-menu []
  (fn [target]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineNewFolderModal"}
      [:a "New Sub-Tag"]]
     [:li.divider]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameModal"}
      [:a "Rename"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteFolderModal"}
      [:a "Remove"]]]))

(defn list-context-menu []
  (fn [target]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameList"}
      [:a "Rename"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineCopyModal"}
      [:a "Duplicate"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteModal"}
      [:a "Delete"]]]))

(defn default-context-menu []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "Default"]]]))

(defn context-menu-container []
  (let [context-menu-target (subscribe [::subs/context-menu-target])]
    (fn []
      (let [{:keys [im-obj-type] :as target} @context-menu-target]
        [:div#contextMenu.dropdown.clearfix
         (case im-obj-type
           "tag" [tag-context-menu target]
           "list" [list-context-menu target]
           [default-context-menu])]))))
