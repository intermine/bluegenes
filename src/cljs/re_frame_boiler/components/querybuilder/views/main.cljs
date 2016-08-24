(ns re-frame-boiler.components.querybuilder.views.main
  (:require-macros [com.rpl.specter.macros :refer [traverse select]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [com.rpl.specter :as s]
            [re-frame-boiler.components.querybuilder.views.constraints :as constraints]
            [json-html.core :as json-html]))

(defn attribute []
  (let [qb-query (subscribe [:query-builder/query])]
    (fn [name & [path]]
      (let [path-vec (conj path name)]
        [:div
         [:div.btn.btn-default.btn.btn-xxs
          {:class    (if (some (fn [x] (= x path-vec)) (:select @qb-query))
                       "btn-primary"
                       "btn-outline")
           :on-click (fn [] (dispatch [:query-builder/add-view path-vec]))}
          [:i.fa.fa-eye]]
         [:div.btn.btn-default.btn-outline.btn-xxs
          {:on-click (fn [] (dispatch [:query-builder/add-filter path-vec]))}
          [:i.fa.fa-plus] [:i.fa.fa-filter]]
         [:span.pad-left-5 name]]))))

(defn tree [class & [path open?]]
  (let [model (subscribe [:model])
        open  (reagent/atom open?)]
    (fn [class]
      [:li
       [:span {:on-click (fn [] (swap! open (fn [v] (not v))))}
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

(defn flat->tree [paths]
  (first (into [] (reduce (fn [total next] (assoc-in total next nil)) {} paths))))

(defn tiny-constraint []
  (fn [details]
    [:span
     [:span.pad-right-5 (str (:op details) " " (:value details))]
     [:span.badge (:code details)]
     [:i.fa.fa-times.pad-left-5
      {:on-click (fn [] (dispatch [:query-builder/remove-constraint details]))}]]))

(defn tree-view []
  (let [query (subscribe [:query-builder/query])]
    (fn [[k v] trail]
      (let [trail (if (nil? trail) [k] trail)]
        [:div
         [:div
          [:span {:class (if (nil? v)
                           (if (not-empty (filter #(= trail %) (:select @query)))
                             "label label-primary"
                             "label label-default"))}
           (str k)
           (into [:span] (map (fn [c] [tiny-constraint c])
                              (filter (fn [t] (= trail (:path t))) (:where @query))))]]
         (if (map? v)
           (into [:ol.tree]
                 (map (fn [m]
                        [:li [tree-view m
                              (conj trail (first m))]]) v)))]))))

(defn main []
  (let [query           (subscribe [:query-builder/query])
        result-count    (subscribe [:query-builder/count])
        counting?       (subscribe [:query-builder/counting?])
        edit-constraint (subscribe [:query-builder/current-constraint])]
    (fn []
      [:div.querybuilder
       [:div.row
        [:div.col-sm-6
         [:div.panel.panel-default
          [:div.panel-heading [:h4 "Data Model"]]
          [:div.panel-body [:ol.tree [tree :Gene ["Gene"] true]]]]]
        [:div.col-sm-6
         [:div.row
          (if @edit-constraint
            [:div.panel [constraints/constraint @edit-constraint]])
          [:div.panel.panel-default
           [:div.panel-heading [:h4 "Query Overview"]]
           [:div.panel-body [tree-view (flat->tree (concat (:select @query) (map :path (:where @query))))]
            [:div
             (if @counting?
               [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
               (if @result-count
                 [:h3 (str @result-count " rows")]))]]]
          [:div.panel.panel-default
           [:div.panel-heading
            [:h4 "Query Structure"]]
           ;[:span (json/edn->hiccup @query)]
           ;[:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
           [:div.panel-body
            [:button.btn.btn-primary {:on-click #(dispatch [:query-builder/reset-query])} "Reset"]]]]]]])))


