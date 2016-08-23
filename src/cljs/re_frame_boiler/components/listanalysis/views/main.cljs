(ns re-frame-boiler.components.listanalysis.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [re-frame-boiler.components.listanalysis.events]
            [re-frame-boiler.components.listanalysis.subs]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(def widget-fields {"Pathway Enrichment" [{:header "Pathway" :field :description}
                                          {:header "Matches" :field :matches}
                                          {:header "p-value" :field :p-value}]})

(defn results []
  (let [results (subscribe [:listanalysis/results])]
    (fn []
      [:div (json-html/edn->hiccup @results)])))

(defn results-row []
  (fn [data]
    (into [:tr]
          (map (fn [{field :field}] [:td (field data)]) (get widget-fields "Pathway Enrichment")))))

(defn results-table []
  (let [results (subscribe [:listanalysis/results])]
    (fn []
      [:table.table
       [:thead
        (into [:tr]
              (map (fn [header] [:th (:header header)]) (get widget-fields "Pathway Enrichment")))]
       (into [:tbody]
             (map (fn [result] [results-row result]) (:results @results)))])))

(defn controls []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-primary
      {:on-click (fn [] (dispatch [:listanalysis/run]))} "Run"]]))

(defn list-analysis []
  (fn [enrichment-type]
    [:div.panel.panel-default.enrichment
     [:div.panel-heading enrichment-type]
     [:div.panel-body
      [:form.form.form-sm
       [:div.row
        [:div.col-sm-5.form-group.form-xs
         [:label.control-label "Test Correction"]
         [:select.form-control
          [:option "Holm-Bonferroni"]
          [:option "Benjamini Hochber"]
          [:option "Bonferroni"]
          [:option "None"]]]
        [:div.col-sm-3.form-group
         [:label.control-label "Max p-value"]
         [:select.form-control
          [:option 0.05]
          [:option 0.10]
          [:option 1.00]]]
        [:div.col-sm-4.form-group
         [:label.control-label "Background Population"]
         [:select.form-control
          [:option 1]
          [:option 2]
          [:option 3]
          [:option 4]]]]]
      [results-table]
      [controls]]

     ;[results]
     ]))

(defn main []
  (fn []
    [:div
     [list-analysis "Pathway Enrichment"]]))