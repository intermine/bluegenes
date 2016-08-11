(ns re-frame-boiler.components.progress_bar
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.components.search :as search]))

(defn main []
  (let [progress-bar-percent (subscribe [:progress-bar-percent])]
    (fn []
     [:div.prog.flash.up-shadow
      {:style {:width (str @progress-bar-percent "%")}}])))