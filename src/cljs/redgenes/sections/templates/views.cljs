(ns redgenes.sections.templates.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.components.templates.views :as templates]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [templates/main])))