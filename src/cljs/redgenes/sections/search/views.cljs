(ns redgenes.sections.search.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.components.search.views :as search]))

(defn main []
  (fn []
     [search/main]))
