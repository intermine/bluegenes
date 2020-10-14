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

(defn report-categories-child [[category-index child-index]
                               {:keys [name type collapse]}]
  (let [dispatch-idx (fn [evt & args]
                       (dispatch (into [evt category-index child-index] args)))]
    [:li.class-entry
     [:div
      [:span name]
      [:span.type type]]
     [:div.btn-group-sm.class-options
      [:button.btn.btn-default.btn-fab
       [poppable {:data [:span (if collapse
                                 "Header will be visible but content needs to be toggled to show"
                                 "Header and content will be visible")]
                  :children [icon (if collapse "eye-blocked" "eye") 2]}]]
      [:button.btn.btn-default.btn-fab
       [icon "move-up-list" 2]]
      [:button.btn.btn-default.btn-fab
       [icon "move-down-list" 2]]
      [:button.btn.btn-default.btn-fab
       [icon "remove-list" 2]]]]))

(defn report-categories-category []
  (let [model (subscribe [:model])
        active* (reagent/atom false)]
    (fn [category-index {:keys [category children]}]
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
           (into [:ul.classes]
                 (concat
                   (for [[i child] (map-indexed vector children)]
                     [report-categories-child [category-index i] child])
                   [[:li.add-child
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
                       [icon "plus"]]]]])))]))))

(defn report-categories-selector []
  (let [categories [{:category "Function"
                     :children [{:name "HPO" :type "class" :collapse true}
                                {:name "Alleles" :type "class"}
                                {:name "Bluegenes GO-Term Visualization" :type "tool"}]}
                    {:category "Disease"
                     :children [{:name "Gene --> RMI" :type "template"}]}]]
    [:div.categories-selector
     (when (seq categories)
       [:em "Click on a category to expand/collapse its children"])
     (into [:ul.nav.nav-pills.nav-stacked]
           (concat
             (for [[i category] (map-indexed vector categories)]
               [report-categories-category i category])
             [[:li.add-category.input-group-sm
               [:input.form-control.input-sm
                {:placeholder "New category"}]
               [:button.btn.btn-default.btn-raised.btn-xs
                "Add category"]]]))]))

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
