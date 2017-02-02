(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]
            [clojure.string :refer [split join]]
            [oops.core :refer [ocall oget]]
            [redgenes.components.ui.constraint :refer [constraint]]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(defn tree-view-recur [model root-class trail selected]
  (let [expanded? (reagent/atom (get-in selected trail))] ; Recursively auto-expand to selected values
    (fn [model root-class trail selected]
      (let [{:keys [displayName attributes collections references] :as p} (get-in model [:classes root-class])]
        [:ul.qb {:class (if (and @expanded? (> (count trail) 1)) "open")}
         [:li {:on-click (fn [] (swap! expanded? not))}
          [:div.nowrap
           (if @expanded?
             [:span.glyphicon.glyphicon-chevron-down]
             [:span.glyphicon.glyphicon-chevron-right])
           [:div.class.nowrap.inlineblock displayName]
           [:div.button-group
            [:button.small-btn {:on-click (fn [e]
                                            (ocall e :stopPropagation)
                                            (.log js/console "CONNING" trail)
                                            (dispatch [:qb/add-constraint trail]))} "Constrain"]]]]
         (if @expanded?
           (concat
             ; Create a map of the attributes
             (map (fn [[_ {:keys [name]}]]
                    (let [selected? (get-in selected (conj trail name))]
                      ^{:key name}
                      [:li {:class (if selected? "selected")
                            :style {:padding-left "35px"}}
                       ;:on-click (fn [] (if selected?
                       ;                   (dispatch [:qb/remove-view (conj trail name)])
                       ;                   (dispatch [:qb/add-view (conj trail name)])))

                       [:span
                        name
                        [:div.button-group
                         (if selected?
                           [:button.small-btn {:on-click (fn [] (dispatch [:qb/remove-view (conj trail name)]))} "Hide"]
                           [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-view (conj trail name)]))} "Show"])
                         [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-constraint (conj trail name)]))} "Constrain"]]]]))

                  (sort attributes))
             ; Combined with a map of collections and references
             (map (fn [[_ colref]]
                    ^{:key (str (name root-class) (:name colref))}
                    [:li [tree-view-recur model (keyword (:referencedType colref)) (conj trail (:name colref)) selected]])
                  (into (sorted-map) (merge collections references)))))]))))

(defn tree-view []
  (fn [m model root-class]
    [tree-view-recur model root-class [(name root-class)] m]))





;(defn query-view []
;  (fn [model m trail]
;    (into [:div.qbcontainer]
;          (->> (into (sorted-map) m) ; Sort our query-map alphabetically
;               (map (fn [[k {:keys [visible children constraints] :as value}]]
;                      (let [path (if trail (conj trail k) [k])]
;                        [:div.qbrow
;                         [:div
;                          [:div.qrow
;                           [:div.button-group {:style {:white-space "nowrap"}}
;
;                            ; Don't show the "visible" icon if this path is a class
;                            (cond
;                              (and (not (p/class? model (join "." path)))
;                                   (not-empty constraints))
;                              [:button.small-btn
;                               {:class    nil ;(if visible "visible")
;                                :on-click (fn [e] (ocall e :stopPropagation) (dispatch [:qb/toggle-view path]))}
;                               (if visible [:i.fa.fa-eye] [:i.fa.fa-eye-slash])]
;                              (and (not (p/class? model (join "." path))) (empty? constraints))
;                              [:span.small-btn.empty [:i.fa.fa-eye]])
;
;                            [:span.key {:class (if visible "attribute")} k
;                             (if-not (empty? trail) ; Don't allow user to drop the root node
;                               [:button.small-btn
;                                {:on-click (fn [e] (ocall e :stopPropagation) (dispatch [:qb/remove-view path]))}
;                                [:i.fa.fa-times]])]]
;
;                           ; Provide an "X" icon to remove the view / constraint
;
;                           (if constraints
;                             (do
;                               (into [:div.ts]
;                                     (map-indexed (fn [idx con]
;                                                    [:div.constraint-row
;
;                                                     [constraint
;                                                      :model model
;                                                      :path (join "." path)
;                                                      :value (:value con)
;                                                      :op (:op con)
;                                                      :on-change (fn [c] (dispatch [:qb/update-constraint path idx c]))
;                                                      :on-blur (fn [c] (println "BLURRY"))
;                                                      :label? false]
;
;                                                     [:button.small-btn.danger
;                                                      {:on-click (fn [] (dispatch [:qb/remove-constraint path idx]))}
;                                                      [:i.fa.fa-times]]])
;
;                                                  constraints))))]]
;                         (if children [:div.qbcol [query-view model children path]])])))))))



(defn table-header []
  [:div.grid
   [:div.col-1] [:div.col-1] [:div.col-5 [:h4 "Field"]] [:div.col-5 [:h4 "Constraints"]]])

(defn constraint-logic-row []
  (let [logic (subscribe [:qb/constraint-logic])]
    (fn []
      [:div.grid
       [:div.col-1]
       [:div.col-1]
       [:div.col-5]
       [:div.col-5.white
        [:label "Logic"]
        [:input.form-control
         {:value     @logic
          :on-change (fn [e] (dispatch [:qb/update-constraint-logic (oget e :target :value)]))
          :on-blur (fn [] (dispatch [:qb/count-query]))
          :type      "text"}]]])))

(defn qb-row []
  (fn [model {:keys [id-count path constraints visible]}]
    (let [lists  (subscribe [:lists])
          class? (p/class? model (join "." path))]
      [:div.grid
       [:div.col-1.white
        {:style {:text-align "right"}}
        [:button.btn.btn-danger.btn-simple
         {:on-click (fn [] (dispatch [:qb/remove-view path]))}
         [:i.fa.fa-times]]]

       ; Controls column
       [:div.col-1.white
        (cond
          ; If we have constraints and we're an attribute allow visibility to be toggled
          (and (not-empty constraints) (not class?))
          [:button.btn.btn-primary.btn-simple
           {:on-click (fn [] (dispatch [:qb/toggle-view path]))}
           (if visible
             [:i.fa.fa-eye]
             [:i.fa.fa-eye-slash])]
          ; If we don't have constraints and we're a class then we can't be visible
          (and (not constraints) class?) nil
          ; Classes can never be visible
          class? nil
          ; Otherwise show a permanently fixed visible icon
          :else [:i.soft.fa.fa-eye])]

       ; Field column
       [:div.col-5.white
        [:span.child {:on-click (fn [x] (dispatch [:qb/summarize-view path]))
                      :class    (if class? "class" "attribute")
                      :style    {:margin-left (str (* 20 (count path)) "px")}}
         [:span (str (last path))]
         (when class?
           (if id-count
             [:span.id-count (str (.toLocaleString (js/parseInt id-count) "en-US"))]
             [:i.fa.fa-cog.fa-spin.fa-1x.fa-fw]))

         #_(str (last path))]]


       ; Constraint column
       [:div.col-5.white
        (if constraints
          (do
            (into [:div.ts]
                  (map-indexed (fn [idx con]
                                 [:div.constraint-row

                                  [:button.btn.btn-danger.btn-simple
                                   {:on-click (fn [] (dispatch [:qb/remove-constraint path idx]))}
                                   [:i.fa.fa-times]]
                                  [:span (:code con)]
                                  [constraint
                                   :model model
                                   :path (join "." path)
                                   :lists (second (first @lists))
                                   :value (:value con)
                                   :op (:op con)
                                   :on-change (fn [c] (dispatch [:qb/update-constraint path idx c]))
                                   :on-blur (fn [] (dispatch [:qb/count-query]))
                                   :label? false]
                                  ])
                               constraints))))
        [:button.btn.btn-success.btn-simple
         {:on-click (fn [] (dispatch [:qb/add-constraint path]))}
         [:span [:i.fa.fa-plus]]]]])))

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
        root-class (subscribe [:qb/root-class])]
    (fn []
      (into [:select.form-control
             {:on-change (fn [e] (dispatch [:qb/set-root-class (oget e :target :value)]))
              :value @root-class}]
            (map (fn [[class-kw details]]
                   [:option {:value class-kw} (:displayName details)])
                 (sort-by (comp :displayName second) (get-in @current-mine [:service :model :classes])))))))

(defn main []
  (let [query           (subscribe [:qb/query])
        flattened-query (subscribe [:qb/flattened])
        current-mine    (subscribe [:current-mine])
        root-class      (subscribe [:qb/root-class])
        query-is-valid? (subscribe [:qb/query-is-valid?])]
    (fn []
      (println "RC" @query-is-valid?)
      [:div.main-window
       [:div.sidex
        [root-class-dropdown]
        [tree-view @query (get-in @current-mine [:service :model]) (keyword @root-class)]]
       [:button.btn.btn-primary.btn-raised
        {:disabled (not @query-is-valid?)
         :on-click (fn [] (dispatch [:qb/export-query]))}
        "View Results"]
       [:button.btn.btn-success
        {:on-click (fn [] (dispatch [:qb/make-query]))}
        "Make Query"]
       [:button.btn.btn-success
        {:on-click (fn [] (dispatch [:qb/mention]))}
        "Mention Query"]
       [:button.btn.btn-success
        {:on-click (fn [] (dispatch [:qb/load-query aquery]))}
        "Example Query"]
       [table-header]
       (into [:div] (map (fn [v] [qb-row (get-in @current-mine [:service :model]) v]) @flattened-query))
       [constraint-logic-row]
       ])))




