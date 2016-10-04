(ns redgenes.components.querybuilder.views.main
  (:require-macros [com.rpl.specter :refer [traverse select]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [com.rpl.specter :as s]
            [clojure.spec :as spec]
            [redgenes.components.querybuilder.core :refer [build-query where-tree]]
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
          {
           :title    (str
                      (if ((:q/select @qb-query) path-vec) "Remove " "Add ")
                       name
                       (if ((:q/select @qb-query) path-vec) " from " " to ")
                       "view")
           :class    (if ((:q/select @qb-query) path-vec)
                       "btn-primary"
                       "btn-outline")
           :on-click (fn [] (dispatch [:query-builder/toggle-view! path-vec]))}
          [:i.fa.fa-eye]]
         [:div.btn.btn-default.btn-outline.btn-xxs
          {
           :title    (str "Add constraint for " name)
           :class
                     (if ((get-in @qb-query [:constraint-paths]) path-vec)
                       "btn-primary"
                       "btn-outline")
           :on-click (fn [] (dispatch [:query-builder/add-filter path-vec]))}
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

(defn tiny-constraint
  [{:keys [:q/op :q/value :q/code] :as constraint} i]
  [:li.qb-tiny-constraint
    {:key i}
   [:span.pad-right-5.qb-constraint-op
    {:on-click (fn [e] (dispatch [:query-builder/set-where-path [:q/where i :q/op]]))}
    (str " " op)]
   [:input.qb-constraint-value
    {:type      :text :value value :default-value 0
     :size      9
     :on-change (fn [e] (dispatch [:query-builder/change-constraint-value i (.. e -target -value)]))}]
   [:span.badge code]
   [:i.fa.fa-times.pad-left-5.buttony
    {:on-click (fn [] (dispatch [:query-builder/remove-constraint constraint i]))}]])

(defn tree-view
  ([query]
    (tree-view query [] (where-tree query)))
  ([query path things]
   (into [:ul.query-tree]
     (map (partial tree-view query things path) things)))
  ([{:keys [:q/select] :as query} things path [k v]]
   (let [path (conj path k)]
    [:li.query-item
     {
      :class
        (if (select path)
        "query-selected" "query-not-selected")
      }
     k
     (if (map? v)
       [tree-view query path v]
       [:ul.query-constraint
        (map
          (fn [c i]
            (with-meta [tiny-constraint c i] {:key i}))
          v (range))])])))

(defn main []
  (let [
        used-codes      (subscribe [:query-builder/used-codes])
        query           (subscribe [:query-builder/query])
        io-query        (subscribe [:query-builder/io-query])
        queried?        (subscribe [:query-builder/queried?])
        result-count    (subscribe [:query-builder/count])
        counting?       (subscribe [:query-builder/counting?])
        autoupdate?     (subscribe [:query-builder/autoupdate?])
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
            [tree-view @query]
            [:textarea
             {
              :cols  128
              :rows  2
              :style {:width  "calc(100% - 1em)" :height "2em"
                      :border :none
                      :margin "1em"
                      :background
                        (if (spec/valid? :q/logic (:q/logic @query)) "rgb(240,240,240)" :pink)}
              :value (:logic-str @query)
              :on-change
                     (fn [e]
                       (dispatch [:query-builder/set-logic! (.. e -target -value)]))
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
            [:div.buttony.autoupdate-button
             {
              :class (if @autoupdate? "selected-button" "")
              :on-click #(dispatch [:query-builder/toggle-autoupdate])} ""]
                [:button.btn.btn-primary {:on-click #(dispatch [:query-builder/reset-query])} "Reset"]
                [:button.btn.btn-primary {:on-click #(dispatch [:query-builder/update-io-query @query])} "Update"]
                [:button.btn.btn-primary {:on-click #(dispatch [:undo])} "Undo"]
                [:button.btn.btn-primary {:on-click #(dispatch [:redo])} "Redo"]
                [:span.btn
                  [:div.undos
                    (map
                     (fn [explanation i]
                       [:div.undo.buttony
                        {:key i
                          :on-click (fn [e] (dispatch [:undo i]))
                         :title    explanation}])
                     @undo-explanations (range (count @undo-explanations) 0 -1))
                     (map
                       (fn [explanation i]
                         [:div.redo.buttony
                          {:key i
                            :on-click (fn [e] (dispatch [:redo i]))
                           :title    explanation}])
                     @redo-explanations (range (count @redo-explanations) 0 -1))]]
            [:div
             (if @counting?
               [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]
               (if @result-count
                 [:h3 (str @result-count " rows")]))]]]
          [:div.panel.panel-default
           [:div.panel-heading
            [:h4 "Results"]]
           ;[:span (json/edn->hiccup @query)]
           ;[:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
           [:div.panel-body
            (cond (not (spec/valid? :q/query @query))
              [:div {} (str (spec/explain-str :q/query @query))])]
           (cond @io-query
            [:div.panel-body
             [table/main @io-query true]])]]]])))