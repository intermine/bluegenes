(ns redgenes.sections.upload.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.components.idresolver.views.main :as idresolver]))

(defn main []
  (fn []
    [:div
     [idresolver/main]]))