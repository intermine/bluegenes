(ns re-frame-boiler.components.listanalysis.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [re-frame-boiler.components.listanalysis.events]
            [re-frame-boiler.components.listanalysis.subs]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn results []
  (let [results (subscribe [:listanalysis/results])]
    (fn []
      [:div (json-html/edn->hiccup @results)])))

(defn controls []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-primary
      {:on-click (fn [] (dispatch [:listanalysis/run]))} "Run"]]))

(defn form []
  (fn []
    [:div.panel
     {:style {:width "50%" :font-size "12px"}}
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
     [results]
     [controls]]))

(defn main []
  (fn []
    [:div
     [form]]))