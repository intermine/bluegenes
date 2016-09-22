(ns redgenes.components.querybuilder.views.main
  (:require-macros [com.rpl.specter :refer [traverse select]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [com.rpl.specter :as s]
            [clojure.spec :as spec]
            [redgenes.components.querybuilder.core :refer [build-query]]
            [redgenes.components.querybuilder.views.constraints :as constraints]
            [redgenes.components.table :as table]
            [json-html.core :as json-html]
            [clojure.string :as string]
            [cljs.spec :as spec]))

(defn attribute []
  (let [qb-query (subscribe [:query-builder/query])]
    (fn [name & [path]]
      (let [path-vec (conj path name)]
        [:div
         [:div.btn.btn-default.btn.btn-xxs
          {:class    (if (some (fn [x] (= x path-vec)) (:q/select @qb-query))
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
       (if @open
        (into [:ul]
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
  (fn [{:keys [:q/op :q/value :q/code] :as details} i]
    [:span.qb-tiny-constraint
     [:span.pad-right-5.qb-constraint-op
      {:on-click (fn [e] (dispatch [:query-builder/set-where-path [:q/where i :q/op]]))}
      (str " " op)]
     [:input.qb-constraint-value
      {:type      :text :value value :default-value 0
        :size 9
       :on-change (fn [e] (dispatch [:query-builder/change-constraint-value i (.. e -target -value)]))}]
     [:span.badge code]
     [:i.fa.fa-times.pad-left-5.buttony
      {:on-click (fn [] (dispatch [:query-builder/remove-constraint details]))}]]))

(defn tree-view []
  (let [query (subscribe [:query-builder/query])]
    (fn [[k v] trail]
      (let [trail (if (nil? trail) [k] trail)]
        [:div
         [:div
          [:span {:class (if (nil? v)
                           (if (not-empty (filter #(= trail %) (:q/select @query)))
                             "label label-primary"
                             "label label-default"))}
           (str k)
           (into [:span] (map (fn [i c] [tiny-constraint c i])
                           (range)
                           (filter (fn [t] (= trail (:q/path t))) (:q/where @query))))]]
         (if (map? v)
           (into [:ol.tree]
             (map (fn [m]
                    [:li [tree-view m
                          (conj trail (first m))]]) v)))]))))

(defn main []
  (let [
        used-codes      (subscribe [:query-builder/used-codes])
        query           (subscribe [:query-builder/query])
        queried?        (subscribe [:query-builder/queried?])
        result-count    (subscribe [:query-builder/count])
        counting?       (subscribe [:query-builder/counting?])
        edit-constraint (subscribe [:query-builder/current-constraint])
        undos?          (subscribe [:undos?])
        redos?          (subscribe [:redos?])
        undo-explanations       (subscribe [:undo-explanations])
        redo-explanations       (subscribe [:redo-explanations])
        ]
    (fn []
      [:div.querybuilder.row
       [:div.col-sm-6
        [:div.panel.panel-default
         [:div.panel-heading [:h4 "Data Model"]]
         [:div.panel-body [:ol.tree [tree :Gene ["Gene"] true]]]]]
         [:div.col-sm-6
         [:div
          (if @edit-constraint
            [:div.panel.panel-default
             [:div.panel-body
              [constraints/constraint @edit-constraint]]])
          [:div.panel.panel-default.full-height
           [:div.panel-heading [:h4 "Query Overview"]]
           [:div.panel-body
            [tree-view (flat->tree (concat (:q/select @query) (map :path (:q/where @query))))]
            [:textarea
             {
              :cols  128
              :rows  4
              :style {:width  "calc(100% - 1em)" :height "4em"
                      :border :none
                      :margin "1em"
                      :background
                              (if (spec/valid? :q/logic (:q/logic @query)) "rgb(240,240,240)" :pink)}
              :value (:logic-str @query)
              :on-change
                     (fn [e]
                       (dispatch [:query-builder/set-logic (.. e -target -value)]))
              }]
              [:div {} @used-codes]
              [:textarea
               {
                :cols  128
                :rows  8
                :style {:width      "calc(100% - 1em)" :height "8em"
                        :border :none
                        :margin "1em"
                        :background
                          (if (spec/valid? :q/query @query) "rgb(240,240,240)" :pink)}
                :value (str @query)
                :on-change
                (fn [e]
                  (dispatch [:query-builder/set-query (.. e -target -value)]))}]
                [:button.btn.btn-primary {:on-click #(dispatch [:query-builder/reset-query])} "Reset"]
                [:button.btn.btn-primary {:on-click #(dispatch [:undo])} "Undo"]
                [:button.btn.btn-primary {:on-click #(dispatch [:redo])} "Redo"]
                [:span.btn
                  [:div.undos
                    (map
                     (fn [explanation i]
                       [:div.undo.buttony
                        {:on-click (fn [e] (dispatch [:undo i]))
                         :title    explanation}])
                     @undo-explanations (range (count @undo-explanations) 0 -1))]]
            (comment [:div
              (if @counting?
                [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
                (if @result-count
                  [:h3 (str @result-count " rows")]))])]]
          [:div.panel.panel-default
           [:div.panel-heading
            [:h4 "Results"]]
           ;[:span (json/edn->hiccup @query)]
           ;[:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
           [:div.panel-body
            (if (spec/valid? :q/query @query)
              [table/main (build-query @query) true]
              [:div {} (str (spec/explain-str :q/query @query))])]]]]])))


