(ns re-frame-boiler.sections.analyse.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.idresolver.views.main :as idresolver]
            [re-frame-boiler.components.listanalysis.views.main :as listanalysis]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [:div.container-fluid
       [:button.btn.btn-default
        {:on-click (fn [] (dispatch [:listanalysis/run-all]))}
        "Run All"]
       [:h2 [:span "List Analysis for "] [:span.stressed (str (:name @params))]]
       [:div.row
        [:div.col-lg-4.col-md-6 [listanalysis/main :pathway_enrichment]]
        [:div.col-lg-4.col-md-6 [listanalysis/main :go_enrichment_for_gene]]
        [:div.col-lg-4.col-md-6 [listanalysis/main :prot_dom_enrichment_for_gene]]
        [:div.col-lg-4.col-md-6 [listanalysis/main :publication_enrichment]]
        [:div.col-lg-4.col-md-6 [listanalysis/main :bdgp_enrichment]]
        [:div.col-lg-4.col-md-6 [listanalysis/main :miranda_enrichment]]]])))