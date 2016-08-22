(ns re-frame-boiler.sections.analyse.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.idresolver.views.main :as idresolver]
            [re-frame-boiler.components.listanalysis.views.main :as listanalysis]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [:div.container-fluid
       [:h2 [:span "List Analysis for "] [:span.stressed (str (:name @params))]]
       [:div.row
        [:div.col-md-12 [listanalysis/main]]]])))