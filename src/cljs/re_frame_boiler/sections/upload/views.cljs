(ns re-frame-boiler.sections.upload.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.idresolver.views.main :as idresolver]))

(defn main []
  (fn []
    [:div
     [idresolver/main]]))