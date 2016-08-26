(ns re-frame-boiler.sections.templates.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.templates.views :as templates]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [templates/main])))