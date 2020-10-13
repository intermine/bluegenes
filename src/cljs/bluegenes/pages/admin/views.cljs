(ns bluegenes.pages.admin.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.pages.admin.events :as events]
            [bluegenes.pages.admin.subs :as subs]
            [bluegenes.pages.querybuilder.views :refer [sort-classes filter-preferred]]
            [oops.core :refer [oget]]))

(defn report-categories-dropdown []
  (let [model @(subscribe [:model])
        categorize-class @(subscribe [::subs/categorize-class])
        classes (sort-classes model)
        preferred (filter-preferred classes)]
    [:div.input-group.categories-dropdown
     [:label.control-label
      {:for "admin__report-category"}
      "Manage categories"]
     (into [:select.form-control
            {:id "admin__report-category"
             :on-change #(dispatch [::events/set-categorize-class (oget % :target :value)])
             :value categorize-class}
            [:option {:value :Default} "Default"]]
           (map (fn [[class-kw details :as item]]
                  (if (map-entry? item)
                    [:option {:value class-kw} (:displayName details)]
                    [:option {:disabled true :role "separator"} "─────────────────────────"]))
                (concat [[:separator]] preferred [[:separator]] classes)))]))


(defn report-categories []
  [:div.well.well-lg
   [:h3 "Report page categories"]
   [:p "Create categories containing classes in the model to be used for the layout of report pages. You can create a " [:strong "default"] " layout that applies to all report page classes, and fine-tune the layout for " [:strong "class-specific"] " report pages (e.g. for class " [:em "Gene"] ")."]
   [report-categories-dropdown]])

(defn main []
  [:div.admin-page.container
   [report-categories]])
