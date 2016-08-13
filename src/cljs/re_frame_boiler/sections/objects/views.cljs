(ns re-frame-boiler.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.search :as search]
            [re-frame-boiler.components.templates.views :as templates]
            [re-frame-boiler.components.lists.views :as lists]))

(defn main []
  (let [params (subscribe [:panel-params])
        summary-fields (subscribe [:summary-fields])]
    (fn []
      [:div
       [:div.container.padme
        [:div.row [:h1 "Report"]]
        [:div.row [:span (str "Object Params: " @params)]]
        [:div.row [:span (str "summy" @summary-fields)]]]])))