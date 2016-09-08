(ns redgenes.sections.explore.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.components.databrowser.views :as databrowser]))

(defn main []
  (fn []
     [databrowser/main]))
