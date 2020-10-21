(ns bluegenes.pages.reportpage.components.table
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]))

(defn section [{:keys [title]} & children]
  (let [collapsed* (reagent/atom false)]
    (fn []
      (into [:div.report-table
             [:h3.report-table-heading
              {:on-click #(swap! collapsed* not)}
              title
              [:button.btn.btn-link.pull-right.collapse-table
               [icon-comp "chevron-up"
                :classes [(when @collapsed* "collapsed")]]]]]
            (when-not @collapsed* children)))))

(defn summary []
  [section
   {:title "Summary"}
   [:div.report-table-body
    [:div.report-table-row
     [:div.report-table-cell.report-table-header
      "Symbol"]
     [:div.report-table-cell
      "BRCA1"]]
    [:div.report-table-row
     [:div.report-table-cell.report-table-header
      "Name"]
     [:div.report-table-cell
      "BRCA1 DNA repair associated"]]
    [:div.report-table-row
     [:div.report-table-cell.report-table-header
      "Primary identifier"]
     [:div.report-table-cell
      "675"]]]])
