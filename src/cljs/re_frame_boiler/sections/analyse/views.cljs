(ns re-frame-boiler.sections.analyse.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.idresolver.views.main :as idresolver]
            [re-frame-boiler.components.table :as table]
            [re-frame-boiler.components.listanalysis.views.main :as listanalysis]))

(defn main []
  (let [params (subscribe [:panel-params])]
    (fn []
      [:div#wrapper
       [:div#sidebar-wrapper
        [:div.container
         [:div.row
          [:div.col-md-12 [:h2 "Enrichment Results"]]]]
        [:div.row
         [:div.col-md-12
          [:div.col-lg-6.col-md-12.col-sm-12 [listanalysis/main :pathway_enrichment]]
          [:div.col-lg-6.col-md-12.col-sm-12 [listanalysis/main :go_enrichment_for_gene]]
          [:div.col-lg-6.col-md-12.col-sm-12 [listanalysis/main :prot_dom_enrichment_for_gene]]
          [:div.col-lg-6.col-md-12.col-sm-12 [listanalysis/main :publication_enrichment]]
          [:div.col-lg-6.col-md-12.col-sm-12 [listanalysis/main :bdgp_enrichment]]
          [:div.col-lg-6.col-md-12.col-md-12 [listanalysis/main :miranda_enrichment]]]]]
       [:div#page-content-wrapper

        [:div.panel.panel-default
         [:div.panel-heading
          [:h2
           [:span "List Analysis for "]
           [:span.stressed (str (or (:name @params) (:temp @params)))]]]
         [:div.panel-body
          [table/main {:from   "Gene"
                       :select ["Gene.symbol"]} true]]]]])))