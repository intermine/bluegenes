(ns bluegenes.components.querybuilder.views.main
  (:require-macros [com.rpl.specter :refer [traverse select]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [com.rpl.specter :as s]
            [clojure.spec :as spec]
            [bluegenes.components.querybuilder.events]
            [bluegenes.components.querybuilder.subs]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.querybuilder.core :as c :refer [build-query where-tree]]
            [bluegenes.components.querybuilder.views.constraints :as constraints]
            [bluegenes.components.table :as table]
            [json-html.core :as json-html]
            [clojure.string :as string]
            [cljs.spec :as spec]))

(defn attribute []
  (let [query (subscribe [:query-builder/query])]
    (fn [{naym :name typ :type} & [path]]
      (let [path-vec (conj path naym)]
        [:div.open-qb-prop
         [:div.view-prop
          {
           :title    (str
                      (if ((:q/select @query) path-vec) "Remove " "Add ")
                       naym
                       (if ((:q/select @query) path-vec) " from " " to ")
                       "view")
           :class    (if ((:q/select @query) path-vec)
                       "selected"
                       "not-selected")
           :on-click
                     (fn []
                       (dispatch [:query-builder/toggle-view! path-vec])
                       (dispatch [:query-builder/update-io-query]))}
          [:i.fa.fa-eye]]
         [:div.constraint
          {
           :title    (str "Add constraint for " naym)
           :class
                     (if ((get-in @query [:constraint-paths]) path-vec)
                     "selected"
                     "not-selected")
           :on-click (fn [] (dispatch [:query-builder/add-filter path-vec typ]))}
          [:i.fa.fa-plus] [:i.fa.fa-filter]]
         [:span.pad-left-5 {:title typ}]naym]))))

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
                  [:li.leaf [attribute details path]])
            (sort (-> @model class :attributes)))
           (map (fn [[_ details]]
                  [tree
                   (keyword (:referencedType details))
                   (conj path (:name details))])
             (sort (-> @model class :collections))))))])))

(defn flat->tree [paths]
  (first (into [] (reduce (fn [total next] (assoc-in total next nil)) {} paths))))

(defn tiny-constraint
  [{:keys [:q/op :q/value :q/code typ] :as constraint} i]
  [:li.qb-tiny-constraint
   [:div.input-group-btn.but-inline
    [:button.btn.btn-default.dropdown-toggle
     {:type        "button"
      :data-toggle "dropdown"}
     op]
    (into
      [:ul.dropdown-menu.pad-right-5.qb-constraint-op {}]
      (map
        (fn [op]
          [:li
           {
            :on-blur
            (fn [e]
              (dispatch [:query-builder/run-query!])
              (dispatch [:query-builder/update-io-query]))
            :on-click
            (fn [e] (dispatch [:query-builder/change-constraint-op i op]))
            } [:a op]])
        (or (constraints/ops-for-type typ) c/ops)))]
   [:input.qb-constraint-value
    {
     :type (constraints/type-for-type typ)
     :value value
     :default-value 0
     :size 9
     :on-change
           (fn [e] (dispatch [:query-builder/change-constraint-value i (.. e -target -value)]))
     :on-blur
           (fn [e]
             (dispatch [:query-builder/run-query!])
             (dispatch [:query-builder/update-io-query]))
     }]
   [:span.badge code]
   [:i.fa.fa-times.pad-left-5.buttony
    {:on-click (fn [] (dispatch [:query-builder/remove-constraint (dissoc constraint :index) i]))}]])

(defn tree-view
  ([query]
    (tree-view query [] (where-tree query)))
  ([query path things]
   (into [:ul.query-tree]
     (map (partial tree-view query things path) things)))
  ([{:keys [:q/select] :as query} th:tokenings path [k v]]
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
          (fn [{i :index :as c}]
            (with-meta [tiny-constraint c i] {:key i}))
          v)])])))

(defn undo-redo-button
  [typ ex i]
  (let [
       {explanation :explanation c :count dc :dcount} (if (map? ex) ex {:explanation ex})
       cc (str "count-" (if (zero? c) 0 (if (neg? dc) "down" (if (zero? dc) "same" "up"))))]
   [:div.buttony
    {:key      i
     :class    (str (name typ) " " cc)
     :on-click (fn [e]
                 (dispatch [typ i])
                 (dispatch [:query-builder/run-query!]))
     :title    (if c
                 (str c (if (== 1 c) " result" " results") ": " explanation)
                 explanation)}]))

; legend

(defn ^:export main []
  (let [
        used-codes      (subscribe [:query-builder/used-codes])
        query           (subscribe [:query-builder/query])
        cl              (subscribe [:query-builder/constraintLogic])
        io-query        (subscribe [:query-builder/io-query])
        result-count    (subscribe [:query-builder/count])
        counting?       (subscribe [:query-builder/counting?])
        edit-constraint (subscribe [:query-builder/current-constraint])
        undos?          (subscribe [:undos?])
        redos?          (subscribe [:redos?])
        undo-explanations       (subscribe [:undo-explanations])
        redo-explanations       (subscribe [:redo-explanations])
        ]
    (fn []
      [:div.row.querybuilder
       [:div.col-sm-6
        [:div.panel.panel-default
         [:div.panel-heading "Data Model"]
         [:div.panel-body [:ol.tree [tree :Gene ["Gene"] true]]]]]
       [:div.col-sm-6
         [:div
          (if @edit-constraint
            [:div.panel.panel-default
             [:div.panel-heading "Constraint"]
             [:div.panel-body
              [constraints/constraint @edit-constraint]]])
          [:div.panel.panel-default.full-height
           [:div.panel-heading "Query Overview"]
           [:div.panel-body
            [tree-view @query]
            [:h5 "Constraint Logic"]
            [:textarea
             {
              :cols  128
              :rows  2
              :style {:width  "calc(100% - 1em)" :height "2em"
                      :border :none
                      :margin "1em"
                      :background
                        (if (spec/valid? :q/logic (:q/logic @query)) "rgb(240,240,240)" "rgb(255,240,240)")}
              :value (:logic-str @query)
              :on-blur
                     (fn [e]
                       (dispatch [:query-builder/run-query!])
                       (dispatch [:query-builder/update-io-query]))
              :on-change
                     (fn [e]
                       (dispatch [:query-builder/set-logic (.. e -target -value)]))
              }]
              [:div {} @used-codes]
            (comment
              [:textarea
              {
               :cols  128
               :rows  8
               :style {:width  "calc(100% - 1em)" :height "8em"
                       :border :none
                       :margin "1em"
                       :background
                               (if (spec/valid? :q/query @query) "rgb(240,240,240)" "rgb(255,240,240)")}
               :value (str @query)
               :on-change
                      (fn [e]
                        (dispatch [:query-builder/set-query (.. e -target -value)]))}])
            [:div.qb-buttons
             [:button.btn.btn-primary {:on-click #(dispatch [:query-builder/reset-query])} "Reset"]
             (if (spec/valid? :q/query @query)
               [:button.btn.btn-primary
                {:on-click #(dispatch [:query-builder/update-io-query])} "Update"]
               [:button.btn.btn-primary.not-working {} "Update"])
             [:button.btn.btn-primary {:on-click #(dispatch [:undo])} "Undo"]
             [:button.btn.btn-primary {:on-click #(dispatch [:redo])} "Redo"]
             [:div.undos
              (map (partial undo-redo-button :undo)
                @undo-explanations (range (count @undo-explanations) 0 -1))
              (map (partial undo-redo-button :redo)
                @redo-explanations (range (count @redo-explanations) 0 -1))]
             ]
            [:div
             (if @counting?
               [loader "preview"]
               (if @result-count
                 [:h3 [:span (str @result-count " rows")] [:span.qb-logic (:constraintLogic (build-query @query))]]))]]]]]
       [:div.col-sm-6
        [:div.panel.panel-default
         [:div.panel-heading "Results"]
         ;[:span (json/edn->hiccup @query)]
         ;[:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
         [:div.panel-body
          (cond (not (spec/valid? :q/query @query))
                [:div {} (str (spec/explain-str :q/query @query))])]
         (cond @io-query
               [:div.panel-body
                [table/main {:query @io-query :service (:service @(subscribe [:current-mine]))} true]])]]])))
