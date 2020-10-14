(ns bluegenes.pages.admin.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.pages.admin.events :as events]
            [bluegenes.pages.admin.subs :as subs]
            [bluegenes.pages.querybuilder.views :refer [sort-classes filter-preferred]]
            [bluegenes.components.icons :refer [icon]]
            [oops.core :refer [oget]]
            [bluegenes.components.bootstrap :refer [poppable]]))

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

(defn report-categories-class [{:keys [class]}]
  [:li.class-entry
   class
   [:div.btn-group-sm.class-options
    [:button.btn.btn-default.btn-fab
     [icon "eye" 2]]
    [:button.btn.btn-default.btn-fab
     [icon "move-up-list" 2]]
    [:button.btn.btn-default.btn-fab
     [icon "move-down-list" 2]]
    [:button.btn.btn-default.btn-fab
     [icon "remove-list" 2]]]])

(defn report-categories-category []
  (let [model (subscribe [:model])
        active* (reagent/atom false)]
    (fn [{:keys [category classes]}]
      (let [remaining-classes (-> @model sort-classes keys)]
        [:li {:class (when @active* :active)}
         [:a {:on-click #(swap! active* not)}
          category
          [:div
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click #(.stopPropagation %)}
            "Rename"]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click #(.stopPropagation %)}
            "Move up"]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click #(.stopPropagation %)}
            "Move down"]
           [:button.btn.btn-default.btn-raised.btn-xs
            {:on-click #(.stopPropagation %)}
            "Delete"]]]
         (when @active*
           [:ul.classes
            (concat
              (for [class classes]
                [report-categories-class class])
              [[:li.add-class
                [:div.full-width
                 [:> js/Select
                  {:placeholder "Available classes, tools and templates"
                   :isMulti true
                   ; :onChange (fn [values]
                   ;             (->> (js->clj values :keywordize-keys true)
                   ;                  (map :value)
                   ;                  (not-empty)
                   ;                  (on-change)))
                   ; :value (map (fn [v] {:value v :label v}) value)
                   :options (map (fn [v] {:value v :label v}) remaining-classes)}]]
                [:div.btn-group-sm
                 [:button.btn.btn-default.btn-fab
                  [icon "plus"]]]]])])]))))

(defn report-categories-selector []
  (let [categories [{:category "Function"
                     :classes [{:class "HPO"}
                               {:class "Alleles"}]}
                    {:category "Disease"}]]
    [:div.categories-selector
     [:em "Click on a category to expand/collapse their children"]
     (into [:ul.nav.nav-pills.nav-stacked]
           (for [category categories]
             [report-categories-category category]))]))

(defn runnable-templates-tooltip []
  [poppable {:data [:div
                    [:p "Some templates can automatically be run on a report page if they meet certain conditions."]
                    [:ol
                     [:li "They must have a single editable constraint"]
                     [:li "That single constraint must be of type LOOKUP"]
                     [:li "That single constraint must be backed by the same class as the item on the report page"]]]
             :children [icon "info"]}])

(defn report-categories []
  [:div.well.well-lg
   [:h3 "Report page categories"]
   [:p "Create categories containing classes (references/collections in the model), tools (visualizations) and/or runnable templates " [runnable-templates-tooltip] ", to be used to determine the layout of report pages. You can create a " [:strong "default"] " layout that applies to all report pages, and fine-tune the layout for " [:strong "class-specific"] " report pages (e.g. for class " [:em "Gene"] ")."]
   [report-categories-dropdown]
   [report-categories-selector]])

(defn main []
  [:div.admin-page.container
   [report-categories]])
