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
            [oops.core :refer [oget ocall]]))

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



(defn feature-branch []
  (let [settings (subscribe [:regions/settings])]
    (fn [[class-kw {:keys [displayName descendants] :as n}]]
      [:li {:class    (if (class-kw (:feature-types @settings)) "selected")
            :on-click (fn [e]
                        (.stopPropagation e)
                        (dispatch [:regions/toggle-feature-type n]))}
       (if (class-kw (:feature-types @settings))
         [:i.fa.fa-check-square-o]
         [:i.fa.fa-square-o])
       displayName
       (if-not (empty? descendants)
         (into [:ul.features-tree] (map (fn [d] [feature-branch d])) (sort-by (comp :displayName second) descendants)))])))

(defn feature-types-tree []
  (let [known-feature-types (subscribe [:regions/sequence-feature-types])
        settings            (subscribe [:regions/settings])]
    (fn []
      (into [:ul.features-tree]
            (map (fn [f] [feature-branch f]) (sort-by (comp :displayName second) @known-feature-types))))))

(def example-regions (clojure.string/join "\n" ["2L:14615455..14619002"
                                                "2R:5866646..5868384"
                                                "3R:2578486..2580016"]))
; Input box for regions
(defn region-input-box []
  (let [to-search (subscribe [:regions/to-search])]
    (fn []
      [:div
       [:textarea.form-control
        {:rows        8
         :placeholder (str "example:\n" example-regions)
         :value       @to-search
         :on-change   (fn [e]
                        (dispatch [:regions/set-to-search (oget e "target" "value")]))}]])))

; Results table
(defn result-table []
  (let [pager (reagent/atom {:show 20
                             :page 0})
        model (subscribe [:model])]
    (fn [{:keys [chromosome from to results] :as feature}]
      [:div.grid-2_xs-1
       {:style {:font-size "0.8em"}}
       [:div.col-3
        [:h4 (str chromosome " " from ".." to)]
        [graphs/main feature]]
       [:div.col-9
        [:div.row
         [:div.btn-toolbar.pull-right
          [:button.btn.btn-primary
           {:disabled (< (:page @pager) 1)
            :on-click (fn [] (swap! pager update :page dec))}
           "Previous"]
          [:button.btn.btn-primary
           {:disabled (< (count results) (* (:show @pager) (inc (:page @pager))))
            :on-click (fn [] (swap! pager update :page inc))} "Next"]]]
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
                            (:end chromosomeLocation))]])
             (take (:show @pager) (drop (* (:show @pager) (:page @pager)) results)))]])))

(defn main []
  (let [results   (subscribe [:regions/results])
        settings  (subscribe [:regions/settings])
        to-search (subscribe [:regions/to-search])]
    (fn []
      [:div.container
       [:div.row
        [:div.col-xs-12
         [:div.panel.panel-default
          [:div.panel-heading "Regions"]
          [:div.panel-body
           [:div.container-fluid
            [:div.row

             [:div.col-xs-4
              [:div.form-group
               [:label "Organism"]
               [im-controls/organism-dropdown
                {:label     (if-let [sn (get-in @settings [:organism :shortName])]
                              sn
                              "All Organisms")
                 :on-change (fn [organism]
                              (dispatch [:regions/set-selected-organism organism]))}]]
              [:div.form-group
               [:label "Regions "
                [popover [:i.fa.fa-question-circle
                          {:data-content   [:span "Genome regions in the following formats are accepted:"
                                            [:ul
                                             [:li [:span "chromosome:start..end, e.g. 2L:11334..12296"]]
                                             [:li [:span "chromosome:start-end, e.g. 2R:5866746-5868284 or chrII:14646344-14667746"]]
                                             [:li [:span "tab delimited"]]]]
                           :data-trigger   "hover"
                           :data-placement "bottom"}]]]
               [region-input-box]
               [:div.btn-toolbar
                [:button.btn.btn-warning
                 {:on-click (fn [] (dispatch [:regions/set-to-search nil]))}
                 "Clear"]
                [:button.btn.btn-primary
                 {:on-click (fn [] (dispatch [:regions/set-to-search example-regions]))}
                 "Example"]]]]
             [:div.col-xs-8

              [:div.form-group
               [:label "Include Features"]
               [:div.feature-tree-container
                [feature-types-tree]]]]]
            [:div.row
             [:div.col-xs-4
              [:div.btn-toolbar]]
             [:div.col-xs-8
              [:div.btn-toolbar
               [:div.btn-toolbar
                [:button.btn.btn-primary
                 {:on-click (fn [] (dispatch [:regions/select-all-feature-types]))}
                 [:span [:i.fa.fa-check-square-o] " Select All"]]
                [:button.btn.btn-primary
                 {:on-click (fn [] (dispatch [:regions/deselect-all-feature-types]))}
                 [:span [:i.fa.fa-square-o] " Deselect All"]]
                [:button.btn.btn-primary.btn-raised.pull-right
                 {:disabled (or
                              (= "" @to-search)
                              (= nil @to-search)
                              (empty? (filter (fn [[name enabled?]] enabled?) (:feature-types @settings))))
                  :on-click (fn [] (dispatch [:regions/run-query]))}
                 [:span "Search"]]]]]]]]]]]
       [:div.row
        [:div.col-xs-12
         [:div.panel.panel-default
          [:div.panel-heading (str "Results (" (apply + (map (comp count :results) @results)) ")")]
          [:div.panel-body
           ; TODO: split this into a view per chromosome, otherwise it doesn't make sense on a linear plane
           #_[:div
              [graphs/main {:results (mapcat :results @results)}]]
           (into [:div]
                 (map (fn [result]
                        [result-table result]) @results))]]]]])))
