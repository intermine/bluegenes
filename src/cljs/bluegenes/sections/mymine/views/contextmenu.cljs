(ns bluegenes.sections.mymine.views.contextmenu
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.events.mymine :as evts]
            [bluegenes.subs.mymine :as subs]
            [oops.core :refer [ocall]]))

(defmulti context-menu :im-obj-type)

(defmethod context-menu "tag" []
  (fn [{:keys [trail type]}]
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
  (let [context-menu-target (subscribe [::subs/context-menu-target])]
    (fn []
      (let [{:keys [entry-id] :as item} @context-menu-target]
        [:div#contextMenu.dropdown.clearfix
         ^{:key (str "context-menu" entry-id)} [context-menu item]]))))