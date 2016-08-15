(ns re-frame-boiler.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.report.summary :as summary]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [:div
       [:div.container.padme
        [summary/main (:summary @report)]]])))