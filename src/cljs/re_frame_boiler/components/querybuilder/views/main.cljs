(ns re-frame-boiler.components.querybuilder.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [re-frame-boiler.components.querybuilder.views.constraints :as constraints]))


(defn attribute []
  (let [qb-query (subscribe [:query-builder-query])]
    (let [state     (reagent/atom false)
          mouseover (reagent/atom false)]
      (fn [name & [path]]
        (let [path-vec (conj path name)]
          [:div
           {:class         (if (some (fn [x] (= x path-vec)) (:select @qb-query)) "active")
            :on-mouse-over (fn [] (reset! mouseover true))
            :on-mouse-out  (fn [] (reset! mouseover false))
            :on-click      (fn [] (dispatch [:qb-add-view path-vec]))}
           name
           (if (or @mouseover @state)
             [:span
              [:i.fa.fa-eye.pad-left
               {:on-click (fn [e]
                            (.stopPropagation e)
                            (dispatch [:qb-add-view path-vec]))}]
              [:i.fa.fa-filter.pad-left]])])))))

(defn tree [class & [path open?]]
  (let [model (subscribe [:model])
        open  (reagent/atom open?)]
    (fn [class]
      ;(println "model" model)
      [:li
       [:div {:on-click (fn [] (swap! open (fn [v] (not v))))}
        (if @open
          [:i.fa.fa-minus-square.pad-right]
          [:i.fa.fa-plus-square.pad-right])
        class]
       (if @open (into [:ul]
                       (concat
                         (map (fn [[_ details]]
                                [:li.leaf [attribute (:name details) path]]) (sort (-> @model class :attributes)))
                         (map (fn [[_ details]]
                                [tree
                                 (keyword (:referencedType details))
                                 (conj path (:name details))]) (sort (-> @model class :collections))))))])))

(defn main []
  (let [query        (subscribe [:query-builder-query])
        result-count (subscribe [:query-builder-count])]
    (fn []
      [:div.querybuilder
       [:div.row
        [:div.col-sm-6
         [:div.panel
          [:h4 "Data Model"]
          [:ol.tree [tree :Gene ["Gene"] true]]]]
        [:div.col-sm-6
         [:div.row
          [:div.panel
           [:h4 "Constraint Testing"]
           [constraints/constraint ["Gene" "name"]]]
          [:div.panel
           [:h4 "Query Structure"]
           [:span (json/edn->hiccup @query)]
           [:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
           [:button.btn.btn-primary {:on-click #(dispatch [:qb-reset-query])} "Reset"]
           [:div (str "count: " @result-count)]]]]]])))


