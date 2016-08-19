(ns re-frame-boiler.components.querybuilder.views.main
  (:require-macros [com.rpl.specter.macros :refer [traverse select]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json]
            [com.rpl.specter :as s]
            [re-frame-boiler.components.querybuilder.views.constraints :as constraints]
            [json-html.core :as json-html]))


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

(def t ["Gene"
        ["name"
         "UTRS"
         ["length"
          "synonys"
          ["value"]]]])

#_(defn overview []
    (fn [query]
      [:span (str (reduce (fn [total next]
                            (loop [f (first next) r (rest next) t total]
                              (let [idx (.indexOf t f)]
                                (println "index" (.indexOf t f))
                                (if (< idx 0) (conj t f) t)))) [] (:select query)))]))

(defn overview []
  (fn [query]
    (let [r (reduce (fn [total next]
                      (assoc-in total next nil)) {} (:select query))]
      (.log js/console "done" r)
      (json-html/edn->hiccup r))))

(defn overview-test []
  (fn [query]
    (let [t ["A"
             ["SUB-A-1"
              "SUB-A-2"
              ["SUB-A-2-1"
               "SUB-A-2-2"
               ["SUB-A-2-2-1"]]]]]

      #_["A"
         ["SUB-A-1"
          "SUB-A-2"
          ["SUB-A-2-1"
           "SUB-A-2-2"
           ["SUB-A-2-2-1"
            ; New:
            "SUB-A-2-2-2"]]]])
    [:span "test"]))

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
           [:h4 "Query Overview"]
           [overview @query]]
          [:div.panel
           [:h4 "Constraint Testing"]
           [constraints/constraint ["Gene" "symbol"]]]
          [:div.panel
           [:h4 "Query Structure"]
           [:span (json/edn->hiccup @query)]
           [:button.btn.btn-primary {:on-click #(dispatch [:qb-run-query])} "Run Count"]
           [:button.btn.btn-primary {:on-click #(dispatch [:qb-reset-query])} "Reset"]
           [:div (str "count: " @result-count)]]]]]])))


