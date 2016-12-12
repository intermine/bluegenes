(ns redgenes.sections.lists.views.operations
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))



(defn operations-bar []
  (let [selected (subscribe [:lists/selected])]
    [:div.btn-toolbar.list-operations
     [:h5 "List tools: "]
     [:div.button-group
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/union]))}
       "Combine"]
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/intersect]))}
       "Intersect"]
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/difference]))}
       "Difference"]
      [:button.btn.delete.btn-default
       {:disabled (empty? @selected)
        :on-click (fn [] (dispatch [:lists/delete]))}
       [:i.fa.fa-trash] " Delete"]]
     [:div.button-group
      [:button.btn.btn-default
       {:disabled (empty? @selected)
        :on-click (fn [] (dispatch [:lists/copy]))}
       [:i.fa.fa-copy] " Copy"]]]))
