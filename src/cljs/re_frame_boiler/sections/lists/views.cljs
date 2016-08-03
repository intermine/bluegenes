(ns re-frame-boiler.sections.lists.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [re-frame-boiler.components.search :as search]
            [re-frame-boiler.components.templates.views :as templates]
            [re-frame-boiler.components.lists.views :as lists]))

(defn main []
  (fn []
    [:div
     [:h1 "Lists"]
     [:div.container.padme]]))