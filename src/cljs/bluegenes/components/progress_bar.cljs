(ns bluegenes.components.progress_bar
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            ))

(defn main []
  (let [progress-bar-percent (subscribe [:progress-bar-percent])]
    (fn []
     [:div.prog.up-shadow
      {:class (if (= 100 @progress-bar-percent) "success" "flash")
       :style {:width (str @progress-bar-percent "%")}}])))
