(ns redgenes.sections.regions.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.sections.regions.graphs :as graphs]
            [redgenes.components.table :as table]
            [redgenes.sections.regions.events]
            [redgenes.sections.regions.subs]
            [redgenes.components.imcontrols.views :as im-controls]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]))

; An individual feature that can be toggled on or off
(defn feature-type []
  (fn [[class-name class-properties] active?]
    [:li
     {:class    (if active? "active")
      :on-click (fn [] (dispatch [:regions/toggle-feature-type class-name]))}
     [:a (:displayName class-properties)]]))

; A collection of all classes that extend SequenceFeature
(defn feature-types []
  (let [known-feature-types (subscribe [:regions/sequence-feature-types])
        settings            (subscribe [:regions/settings])]
    (fn []
      (into [:ul.nav.nav-pills
             {:style {:font-size "0.8em"}}]
            (map (fn [t]
                   (let [active? (get-in @settings [:feature-types (first t)])]
                     [feature-type t active?]))
                 (sort-by (comp :displayName second) @known-feature-types))))))

(def example-regions (clojure.string/join "\n" ["2L:14615455..14619002"
                                                "2R:5866646..5868384"
                                                "3R:2578486..2580016"]))
; Input box for regions
(defn region-input-box []
  (let [to-search (subscribe [:regions/to-search])]
    (fn []
      [:div
       [:textarea.form-control
        {:rows      4
         :value     @to-search
         :on-change (fn [e]
                      (dispatch [:regions/set-to-search (oget e "target" "value")]))}]])))

; Results table
(defn result-table []
  (let [model (subscribe [:model])]
    (fn [{:keys [chromosome from to results] :as feature}]
      [:div.grid-2_xs-1
       {:style {:font-size "0.8em"}}
       [:div.col-3
        [:h4 (str chromosome " " from ".." to)]
        [graphs/main feature]]
       [:div.col-9
        [:div.grid-3_xs-3
         [:div.col [:h4 "Feature"]]
         [:div.col [:h4 "Feature Type"]]
         [:div.col [:h4 "Location"]]]
        (map (fn [{:keys [primaryIdentifier class chromosomeLocation] :as result}]
               [:div.grid-3_xs-3
                [:div.col {:style {:word-wrap "break-word"}} primaryIdentifier]
                [:div.col (str (get-in @model [(keyword class) :displayName]))]
                [:div.col (str
                            (get-in chromosomeLocation [:locatedOn :primaryIdentifier])
                            ":"
                            (:start chromosomeLocation)
                            ".."
                            (:end chromosomeLocation))]]) results)]])))

(defn main []
  (let [results  (subscribe [:regions/results])
        settings (subscribe [:regions/settings])]
    (fn []
      [:div.container
       [:div.row
        [:div.col-xs-12
         [:div.panel1.panel-default1
          [:div.panel-heading1 "Regions"]
          [:div.panel-body1
           [:div.container-fluid
            [:div.row
             [:div.col-xs-8
              [:div.form-group
               [:label "Organism"]
               [im-controls/organism-dropdown
                {:label     (if-let [sn (get-in @settings [:organism :shortName])]
                              sn
                              "All Organisms")
                 :on-change (fn [organism]
                              (dispatch [:regions/set-selected-organism organism]))}]]
              [:div.form-group
               [:label "Include Features"]
               [feature-types]]]
             [:div.col-xs-4
              [:div.form-group
               [:label "Regions"]
               [region-input-box]
               [:div.btn-toolbar
                [:button.btn.btn-primary
                 {:on-click (fn [] (dispatch [:regions/set-to-search example-regions]))}
                 "Example"]]]]]
            [:div.row
             [:div.col-xs-8
              [:div.btn-toolbar
               [:button.btn.btn-primary
                {:on-click (fn [] (dispatch [:regions/select-all-feature-types]))}
                "Select All"]
               [:button.btn.btn-primary
                {:on-click (fn [] (dispatch [:regions/deselect-all-feature-types]))}
                "Deselect All"]]]
             [:div.col-xs-4
              [:div.btn-toolbar
               [:button.btn.btn-primary
                {:on-click (fn [] (dispatch [:regions/run-query]))}
                "Search"]]]]]]]]]
       [:div.row
        [:div.col-xs-12
         [:div.panel.panel-default
          [:div.panel-heading (str "Results (" (apply + (map (comp count :results) @results)) ")")]
          [:div.panel-body
           ; TODO: split this into a view per chromosome, otherwise it doesn't make sense on a linear plane
           #_[:div
            [graphs/main {:results (mapcat :results @results)} ]]
           (into [:div]
                 (map (fn [result]
                        [result-table result]) @results))]]]]])))
