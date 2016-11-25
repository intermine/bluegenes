(ns redgenes.sections.lists.views.operations
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))



(defn operations-bar []
  [:div.btn-toolbar
   [:button.btn.btn-default
    {:on-click (fn [] (dispatch [:lists/union]))}
    "Combine"]
   ;[:button.btn.btn-default "Intersect"]
   ;[:button.btn.btn-default "Unique"]
   ;[:button.btn.btn-default "Subtract"]
   [:button.btn.btn-warning
    {:on-click (fn [] (dispatch [:lists/delete]))}
    "Delete"]])