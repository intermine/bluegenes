(ns re-frame-boiler.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.search :as search]
            [re-frame-boiler.components.templates.views :as templates]
            [re-frame-boiler.components.lists.views :as lists]))

(defn main []
  (let [params (subscribe [:panel-params])
        assets (subscribe [:lists])]
    (fn []
      [:div
       [:div.container.padme
        [:div.row [:h1 "Report"]]
        [:div.row [:span (str "Display asset: " @params)]]
        [:div.row [:span
                   (filter #(= "PL FlyAtlas_tubules_top" (:name %))@assets)]]
        ;[:div.row [:span (str @assets)]]
        ]])))


