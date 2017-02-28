(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]
            [clojure.string :refer [split join blank?]]
            [oops.core :refer [ocall oget]]
            [clojure.string :as str :refer [starts-with? ends-with?]]
            [redgenes.utils :refer [uncamel]]
            [redgenes.components.bootstrap :refer [tooltip-new]]
            [redgenes.components.ui.constraint :refer [constraint]]
            [imcljs.path :as im-path]
            [cljs.reader :refer [read]]
            [cljs.reader :refer [read-string]]
            [redgenes.components.ui.results_preview :refer [preview-table]]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(def auto (reagent/atom true))





(def aquery {:from            "Gene"
             :constraintLogic "A or B"
             :select          ["symbol"
                               "organism.name"
                               "alleles.name"
                               "alleles.dataSets.description"]
             :where           [{:path  "Gene.symbol"
                                :op    "="
                                :code  "A"
                                :value "zen"}
                               {:path  "Gene.symbol"
                                :op    "="
                                :code  "B"
                                :value "mad"}]})

(defn root-class-dropdown []
  (let [current-mine (subscribe [:current-mine])
        root-class   (subscribe [:qb/root-class])]
    (fn []
      (into [:select.form-control
             {:on-change (fn [e] (dispatch [:qb/set-root-class (oget e :target :value)]))
              :value     @root-class}]
            (map (fn [[class-kw details]]
                   [:option {:value class-kw} (:displayName details)])
                 (sort-by (comp :displayName second) (get-in @current-mine [:service :model :classes])))))))




(defn dotsplit [string] (split string "."))

(defn im-query->map [{:keys [select]}]
  (reduce (fn [total next] (assoc-in total next {})) {} (map dotsplit select)))

(defn attributes-first [[_ v]] (empty? v))

(defn has-children? [[_ v]] (some? (not-empty v)))

(defn within? [haystack needle] (some? (some #{needle} haystack)))

(defn not-selected [selected [k attributes-map]]
  (not (within? selected (name k))))

(def q {:from   "Gene"
        :select ["Gene.symbol"
                 "Gene.secondaryIdentifier"
                 "Gene.organism.name"]})

(defn attribute []
  (let [mappy (subscribe [:qb/mappy])]
    (fn [model [k properties] & [trail sub]]
      (let [path      (conj trail (name k))
            selected? (get-in @mappy path)]
        [:li.haschildren
         [:p
          [:span
           {:on-click (fn []
                        (if selected?
                          (dispatch [:qb/mappy-remove-view path sub])
                          (dispatch [:qb/mappy-add-view path sub])))}
           (if (get-in @mappy path)
             [:i.fa.fa-check-square-o.fa-fw]
             [:i.fa.fa-square-o.fa-fw.light])
           [:span.qb-label (str " " (uncamel (:name properties)))]]]]))))



(defn node []
  (let [menu              (subscribe [:qb/menu])]
    (fn [model [k properties] & [trail]]
      (let [path     (vec (conj trail (name k)))
            open?    (get-in @menu path)
            str-path (join "." path)
            sub      (get-in @menu (conj path :subclass))]


        [:li.haschildren
         [:p
          [:div
           {:style    {:white-space "nowrap"}
            :on-click (fn []
                        (if open?
                          (dispatch [:qb/collapse-path path])
                          (dispatch [:qb/expand-path path])))}
           [:i.fa.fa-plus.fa-fw.arr.semilight {:class (when open? "arrow-down")}]
           [:span.qb-class (uncamel (:name properties))]
           [:span.label-button
            {:on-click (fn [e]
                         (.stopPropagation e)
                         (dispatch [:qb/mappy-add-summary-views path sub]))}
            "Summary"]]]
         (when open?
           (into [:ul]
                 (concat
                   (when (im-path/class? model str-path)
                     (when-let [subclasses (im-path/subclasses model str-path)]
                       (list
                         [:li [:p
                               [:span
                                (into
                                  [:select.form-control
                                   {:on-change (fn [e] (dispatch [:qb/mappy-choose-subclass path (oget e :target :value)]))}]
                                  (map (fn [subclass] [:option {:value subclass} (uncamel (name subclass))]) (conj subclasses (:referencedType properties))))]]])))
                   (if sub
                     (map (fn [i] [attribute model i path sub]) (sort (remove (comp (partial = :id) first) (im-path/attributes model sub))))
                     (map (fn [i] [attribute model i path sub]) (sort (remove (comp (partial = :id) first) (im-path/attributes model (:referencedType properties))))))
                   (if sub
                     (map (fn [i] [node model i path false]) (sort (im-path/relationships model sub)))
                     (map (fn [i] [node model i path false]) (sort (im-path/relationships model (:referencedType properties))))))))]))))

(defn model-browser []
  (fn [model root-class]
    [:div.model-browser
     (let [path [root-class]]
       (into [:ul
              [:li [:div
                    {:style {:white-space "nowrap"}}
                    [:button.btn.btn-default.btn-slim
                     {:on-click (fn [] (dispatch [:qb/mappy-add-summary-views [root-class]]))}
                     [:span.label-button

                      "Summary"]]
                    [:button.btn.btn-primary.btn-slim
                     {:on-click (fn [] (dispatch [:qb/expand-all]))}
                     "Expand to Selection"]
                    [:button.btn.btn-primary.btn-slim
                     {:on-click (fn [] (dispatch [:qb/collapse-all]))}
                     "Collapse All"]]]]
             ;(.log js/console "ATTRIBUTES" (im-path/attributes model root-class))
             (concat
               (map (fn [i] [attribute model i path]) (sort (remove (comp (partial = :id) first) (im-path/attributes model root-class))))
               (map (fn [i] [node model i path]) (sort (im-path/relationships model root-class))))))]))


(defn dissoc-keywords [m]
  (apply dissoc m (filter keyword? (keys m))))

(defn queryview-node []
  (fn [model [k properties] & [trail]]
    ;(.log js/console "k m" k trail)
    (let [path (vec (conj trail (name k)))]
      [:li.tree.haschildren
       [:p.flexmex
        [:span.lab {:class (if (im-path/class? model (join "." path)) "qb-class" "qb-attribute")}
         [:span.qb-label {:style {:margin-left 5}} [:a (uncamel k)]]
         (when-let [s (:subclass properties)] [:span.label.label-default (uncamel s)])
         [:i.fa.fa-trash-o.fa-fw.danger
          {:on-click (if (> (count path) 1)
                       (fn [] (dispatch [:qb/mappy-remove-view path]))
                       (fn [] (dispatch [:qb/mappy-clear-query path])))}]
         [:span
          {:on-click (fn [] (dispatch [:qb/mappy-add-constraint path]))}
          [:span [:span [:i.fa.fa-filter.semilight]]]]
         (when-let [c (:id-count properties)]
           [:span.label.label-soft
            {:class (when (= 0 c) "label-no-results")
             :style {:margin-left 5}} (str c " row" (when (not= c 1) "s"))])]]
       (when-let [constraints (:constraints properties)]
         (into [:ul.tree.banana]
               (map-indexed (fn [idx con]
                              [:li
                               [:p
                                [:div.contract
                                 {:class (when (empty? (:value con)) "empty")}
                                 [constraint
                                  :model model
                                  :path (join "." path)
                                  ;:lists (second (first @lists))
                                  :code (:code con)
                                  :on-remove (fn [] (dispatch [:qb/mappy-remove-constraint path idx]))
                                  ;:possible-values (map :value possible-values)
                                  :value (:value con)
                                  :op (:op con)
                                  :on-change (fn [c]
                                               (dispatch [:qb/mappy-update-constraint path idx c]))
                                  :on-blur (fn [x] (dispatch [:qb/mappy-build-im-query]))
                                  ;(dispatch [:qb/build-im-query])

                                  :label? false]]]]) constraints)))
       (when (not-empty (dissoc-keywords properties))
         (let [classes    (filter (fn [[k p]] (im-path/class? model (join "." (conj path k)))) (dissoc-keywords properties))
               attributes (filter (fn [[k p]] ((complement im-path/class?) model (join "." (conj path k)))) (dissoc-keywords properties))]
           (into [:ul.tree.banana2]
                 (concat
                   (map (fn [n] [queryview-node model n path]) (sort attributes))
                   (map (fn [n] [queryview-node model n path]) (sort classes))))))])))

(defn queryview-browser []
  (let [mappy (subscribe [:qb/mappy])]
    (fn [model]
      (if (not-empty @mappy)
        [:div.query-browser
         (into [:ul.tree]
               (map (fn [n]
                      [queryview-node model n]) @mappy))]
        [:div
         [:div "Please select at least one attribute from the Model Browser."]
         [:button.btn.btn-primary.btn-raised
          {:on-click (fn [] (dispatch [:qb/load-query aquery]))}
          ;{:on-click (fn [] (println "finished" (listify nil)))}

          "Example"]]))))

(def <sub (comp deref subscribe))

(defn logic-box []
  (let [logic (subscribe [:qb/constraint-logic])]
    (fn []
      [:div
       [:input.form-control
        {:style     {:max-width "300"}
         :type      "text"
         :value     @logic
         :on-blur   (fn [] (dispatch [:qb/format-constraint-logic @logic]))
         :on-change (fn [e] (dispatch [:qb/update-constraint-logic (oget e :target :value)]))}]])))

(defn controls []
  [:div.button-group
   [:button.btn.btn-primary.btn-raised
    {:on-click (fn [] (dispatch [:qb/export-query]))}
    "Show Results"]
   [:button.btn.btn-danger
    {:on-click (fn [] (dispatch [:qb/mappy-clear-query]))}
    "Clear Query"]])

(defn preview [result-count]
  (let [results-preview   (subscribe [:qb/preview])
        fetching-preview? (subscribe [:idresolver/fetching-preview?])]
    [preview-table
     :loading? @fetching-preview?
     :query-results @results-preview]))

(defn drop-nth [n coll] (vec (keep-indexed #(if (not= %1 n) %2) coll)))

(defn sortable-list []
  (let [order (subscribe [:qb/order])
        state (reagent/atom {:items ["A" "B" "C" "D"] :selected nil})]
    (fn []
      (into [:div.sort-order-container
             {:class (when (some? (:selected @state)) "dragtest")}]
            (map-indexed
              (fn [idx i]
                [:div {:class         (when (= idx (:selected @state)) "dragging")
                       :draggable     true
                       :on-drag-start (fn [e]
                                        ;(.preventDefault e)
                                        (.stopPropagation e)
                                        (ocall e "dataTransfer.setData" "banana" "cakes")
                                        (swap! state assoc :selected idx))
                       :on-drag-enter (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (let [selected-idx  (:selected @state)
                                              items         @order
                                              selected-item (get items selected-idx)
                                              [before after] (split-at idx (drop-nth selected-idx items))]
                                          #_(swap! state assoc
                                                   :selected idx
                                                   :items (vec (concat (vec before) (vec (list selected-item)) (vec after))))
                                          (swap! state assoc :selected idx)
                                          (dispatch [:qb/set-order (vec (concat (vec before) (vec (list selected-item)) (vec after)))])))
                       :on-drag-end   (fn [] (swap! state assoc :selected nil))}
                 (into [:div] (map (fn [part] [:span.part part]) (interpose ">" (map uncamel (split i ".")))))])) @order
            ))))

(defn main []
  (let [mappy        (subscribe [:qb/mappy])
        query        (subscribe [:qb/query])
        current-mine (subscribe [:current-mine])
        root-class   (subscribe [:qb/root-class])]
    (reagent/create-class
      {:component-did-mount (fn [x]
                              (when (empty? @query)
                                (dispatch [:qb/set-root-class "Gene"])))
       :reagent-render      (fn []
                              [:div.column-container
                               [:div.model-browser-column
                                [:div.container-fluid
                                 [:h4 "Model Browser"]
                                 #_[:div.btn-toolbar
                                    [:button.btn.btn-primary.btn-slim
                                     {:on-click (fn [] (dispatch [:qb/expand-all]))}
                                     "Expand to Query"]
                                    [:button.btn.btn-primary.btn-slim
                                     {:on-click (fn [] (dispatch [:qb/collapse-all]))}
                                     "Collapse All"]]
                                 [:div
                                  [:span "Start with..."]
                                  ]
                                 [root-class-dropdown]
                                 [model-browser (:model (:service @current-mine)) (name @root-class)]]]
                               [:div.query-view-column
                                [:div.container-fluid
                                 [:div.row
                                  [:div.col-md-6
                                   [:div
                                    [:h4 "Query"]
                                    [queryview-browser (:model (:service @current-mine))]
                                    (when (not-empty @mappy)
                                      [:div
                                       [:h4 "Constraint Logic"]
                                       [logic-box]])
                                    ]]
                                  (when (not-empty @mappy)
                                    [:div.col-md-6
                                     [:div
                                      [:div
                                       [:h4 "Column Order"]
                                       [sortable-list]]
                                      [controls]
                                      ]])]]]])})))




;[:h4 "Preview"]

;[preview]




(defn toggle-all-checkbox []
  [:span
   [:input {:type    "checkbox"
            :checked @(subscribe [:todos/all-complete?])}]
   [:label {:for "toggle-all"} "Mark all as complete"]])