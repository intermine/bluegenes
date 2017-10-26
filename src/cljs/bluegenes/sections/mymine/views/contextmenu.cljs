(ns bluegenes.sections.mymine.views.contextmenu
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]))

(defmulti context-menu :file-type)

(defmethod context-menu :folder []
  (fn [{:keys [trail type]}]
    [:ul.dropdown-menu
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineNewFolderModal"}
      [:a "New Folder"]]
     [:li.divider]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameModal"}
      [:a "Rename"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteFolderModal"}
      [:a "Remove"]]]))

(defmethod context-menu :list []
  (fn [target]
    [:ul.dropdown-menu
     #_[:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineRenameList"}
      [:a "Rename"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineCopyModal"}
      [:a "Copy"]]
     [:li {:data-toggle "modal"
           :data-keyboard true
           :data-target "#myMineDeleteModal"}
      [:a "Delete"]]]))

(defmethod context-menu :default []
  (fn [target]
    [:ul.dropdown-menu
     [:li [:a "Default"]]]))

(defn context-menu-container []
  (let [menu-item (subscribe [::subs/menu-details])]
    (fn []
      (let [{:keys [trail id file-type] :as item} @menu-item]
        [:div#contextMenu.dropdown.clearfix ^{:key (str "context-menu" trail)} [context-menu item]]))))