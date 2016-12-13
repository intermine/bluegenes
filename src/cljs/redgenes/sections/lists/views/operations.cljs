(ns redgenes.sections.lists.views.operations
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))



(defn operations-bar []
  (let [selected (subscribe [:lists/selected])]
    [:div.btn-toolbar.list-operations
     [:h5 "List tools: "]
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/union]))}
       [:svg.icon.icon-venn-combine [:use {:xlinkHref "#icon-venn-combine"}]]
       "Combine"]
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/intersect]))}
       [:svg.icon.icon-venn-intersection [:use {:xlinkHref "#icon-venn-intersection"}]]"Intersect"]
      [:button.btn.btn-default
       {:disabled (< (count @selected) 2)
        :on-click (fn [] (dispatch [:lists/difference]))}
       [:svg.icon.icon-venn-difference [:use {:xlinkHref "#icon-venn-difference"}]]
       "Difference"]

       [:div [:button.btn.btn-default
       {:disabled (empty? @selected)
         :on-click (fn [] (dispatch [:lists/copy]))}
         [:i.fa.fa-copy] " Copy"]
      [:button.btn.delete.btn-default
       {:disabled (empty? @selected)
        :on-click (fn [] (dispatch [:lists/delete]))}
       [:i.fa.fa-trash] " Delete"]]]
     ))
