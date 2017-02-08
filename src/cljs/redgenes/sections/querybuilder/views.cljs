(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]
            [clojure.string :refer [split join blank?]]
            [oops.core :refer [ocall oget]]
            [clojure.string :as str]
            [redgenes.utils :refer [uncamel]]
            [redgenes.components.bootstrap :refer [tooltip-new]]
            [redgenes.components.ui.constraint :refer [constraint]]
            [imcljs.path :as im-path]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(defn tree-view-recur [model root-class trail selected & [override]]
  (let [expanded? (reagent/atom (get-in selected trail))] ; Recursively auto-expand to selected values
    (fn [model root-class trail selected]
      (let [{:keys [displayName attributes collections references] :as p} (get-in model [:classes root-class])]
        [:ul.qb {:class (if (and @expanded? (> (count trail) 1)) "open")}
         [:li {:on-click (fn [] (swap! expanded? not))}
          [:div.nowrap
           (if @expanded?
             [:span.glyphicon.glyphicon-chevron-down]
             [:span.glyphicon.glyphicon-chevron-right])
           [:div.button-group
            [:button.small-btn {:on-click (fn [e]
                                            (ocall e :stopPropagation)
                                            (dispatch [:qb/add-constraint trail]))} [:i.fa.fa-filter]]]
           [:div.class.nowrap.inlineblock (or override displayName)]]]


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

                       [:div.button-group
                        [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-constraint (conj trail name)]))} [:i.fa.fa-filter]]
                        (if selected?
                          [:button.small-btn {:on-click (fn [] (dispatch [:qb/remove-view (conj trail name)]))} [:i.fa.fa-minus]]
                          [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-view (conj trail name)]))} [:i.fa.fa-plus]])]

                       [:span
                        (uncamel name)]]))

                  (sort (dissoc attributes :id)))
             ; Combined with a map of collections and references
             (map (fn [[_ colref]]
                    ^{:key (str (name root-class) (:name colref))}
                    [:li [tree-view-recur model (keyword (:referencedType colref)) (conj trail (:name colref)) selected (uncamel (:name colref))]])
                  (into (sorted-map) (merge collections references)))))]))))

(defn tree-view []
  (fn [m model root-class]
    (when root-class
      [tree-view-recur model root-class [(name root-class)] m])))

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
          :on-blur   (fn [] (dispatch [:qb/count-query]))
          :type      "text"}]]])))

(defn qb-row []
  (fn [model {:keys [id-count path constraints visible possible-values]}]
    (let [lists  (subscribe [:lists])
          class? (p/class? model (join "." path))]
      [:div.grid
       [:div.col-1.white
        {:style {:text-align "right"}}
        (when (> (count path) 1)
          [:button.btn.btn-danger.btn-simple
           {:on-click (fn [] (dispatch [:qb/remove-view path]))}
           [:i.fa.fa-times]])]

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
         [:span (str (uncamel (last path)))]
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
                                   :code (:code con)
                                   :possible-values (map :value possible-values)
                                   :value (:value con)
                                   :op (:op con)
                                   :on-change (fn [c] (dispatch [:qb/update-constraint path idx c]))
                                   :on-blur (fn [x]
                                              (dispatch [:qb/build-im-query]))
                                   :label? false]])

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

(defn atts-old []
  (fn [model path-vec selected]
    (let [attributes (im-path/attributes model (join "." path-vec))
          unselected (doall (filter (partial not-selected (map first selected)) attributes))]
      (into [:div.attributes]
            (map (fn [[field-kw properties]]
                   [:div
                    {:on-click (fn []
                                 (dispatch [:qb/mappy-add-view (conj path-vec (name field-kw))]))}
                    [:i.fa.fa-square-o.fa-fw]
                    (uncamel (name field-kw))])
                 unselected)))))

(defn atts []
  (fn [model path-vec selected]
    (let [attributes (im-path/attributes model (join "." path-vec))
          selected   (map first selected)]
      (into [:div.attributes]
            (map (fn [[field-kw properties]]
                   (let [selected? (within? selected (name field-kw))]
                     (if selected?
                       [:div
                        [:span.ex
                         {:on-click (fn [] (dispatch [:qb/mappy-remove-view (conj path-vec (name field-kw))]))}
                         [:i.fa.fa-check-square-o.fa-fw]
                         (uncamel (name field-kw))]]
                       [:div
                        [:span.ex
                         {:on-click (fn [] (dispatch [:qb/mappy-add-view (conj path-vec (name field-kw))]))}
                         [:i.fa.fa-square-o.fa-fw]
                         (uncamel (name field-kw))]]
                       )))
                 (sort attributes))))))


(defn rels []
  (fn [model path-vec]
    (let [relationships (im-path/relationships model (join "." path-vec))]
      (if (not-empty relationships)
        [:div.attributes
         (into [:div.attributes]
               (map (fn [[kw properties]]
                      [:div
                       [:span
                        [:i.fa.fa-plus]
                        (str " " (uncamel (name kw)))]]) relationships))]
        [:div "(No relationships)"]))))


(defn constr []
  (fn [model path]
    [constraint
     :model model
     :path (join "." path)
     :label? false]))



(defn tree []
  (let [editing?    (reagent/atom false)
        adding-rel? (reagent/atom false)]
    (fn [m model & [trail]]

      (let [with-no-children (filter (complement has-children?) m)
            with-children    (filter has-children? m)]

        (into [:ul]

              (concat

                ; Then a button to show the catalog
                (when trail (conj '() [:div
                                       [:div.btn-toolbar
                                        [:li.haschildren
                                         [:p
                                          [:button.btn.btn-primary.il
                                           {:class    (if @editing? "btn-raised")
                                            :on-click (fn [] (swap! editing? not))}
                                           [:i.fa.fa-pencil] " Attributes"
                                           ]]]
                                        [:li.haschildren
                                         [:p
                                          [:button.btn.btn-primary.il
                                           {:class    (if @adding-rel? "btn-raised")
                                            :on-click (fn [] (swap! adding-rel? not))}
                                           [:i.fa.fa-share-alt] " Relationships"
                                           ]]]]]))

                ; Attributes first!
                (if (and trail @editing?)

                  ; If editing then show the attribute catalog
                  (list [:li.haschildren [:p [atts model trail with-no-children]]])
                  ; Otherwise just show our attributes
                  (->> (sort with-no-children)
                       (map (fn [[k v]]
                              [:li.haschildren
                               [:p
                                [:span (str " " (uncamel k))]]]))))

                (list [:li.haschildren [:p [:span.hotlink [:a "ID"] [:a "Primary Identifier"] [:a "Another thing"]]
                                          ]])



                ; Then collections!
                (if @adding-rel?

                  (list [:li.haschildren [:p [rels model trail]]])

                  (->> with-children
                       (map (fn [[k v]]
                              (let [loc (vec (conj trail k))]
                                [:li.haschildren
                                 [:p {:class "classy"}
                                  k
                                  ;[:button.btn.btn-primary.il [:i.fa.fa-share-alt]]
                                  #_[:button.btn.btn-primary.il
                                     {:on-click (fn [] (swap! adding-rel? not))}
                                     [:i.fa.fa-share-alt] " Add Relationship "]
                                  ]
                                 [:div
                                  [tree v model loc]]])))))))))))


(def q {:from   "Gene"
        :select ["Gene.symbol"
                 "Gene.secondaryIdentifier"
                 "Gene.organism.name"]})


(defn main []
  (let [query           (subscribe [:qb/query])
        flattened-query (subscribe [:qb/flattened])
        current-mine    (subscribe [:current-mine])
        root-class      (subscribe [:qb/root-class])
        query-is-valid? (subscribe [:qb/query-is-valid?])
        mappy           (subscribe [:qb/mappy])]
    (reagent/create-class
      {:component-did-mount (fn [x]
                              (when (empty? @query)
                                (dispatch [:qb/set-root-class "Gene"])))
       :reagent-render      (fn []
                              [:div.container

                               [:div.playground
                                [:span "Query"]
                                ;(prn "USING MAP" (im-query->map q))
                                [tree @mappy (:model (:service @current-mine))]]


                               ;.main-window
                               #_[:div.sidex



                                  [:button.btn.btn-success
                                   {:on-click (fn [] (.log js/console "M" (im-query->map q)))}
                                   "Do It"]


                                  [root-class-dropdown]
                                  [tree-view @query (get-in @current-mine [:service :model]) (keyword @root-class)]]

                               ;[:button.btn.btn-success
                               ; {:on-click (fn [] (dispatch [:qb/load-query aquery]))}
                               ; "Example Query"]
                               ;
                               ;(println "giving tree" (im-query->map q))




                               [table-header]
                               (into [:div] (map (fn [v] [qb-row (get-in @current-mine [:service :model]) v]) @flattened-query))
                               [constraint-logic-row]

                               (if @query-is-valid?
                                 [:button.btn.btn-primary.btn-raised
                                  {:on-click (fn [] (dispatch [:qb/export-query]))} "View Results"]
                                 [tooltip-new {:title          "Please select at least one visible attribute."
                                               :data-trigger   "hover"
                                               :data-placement "bottom"}
                                  [:button.btn.btn-primary.btn-raised
                                   {:class "disabled"} "View Results"]])])})))






#_(defn tree []
    (let [editing? (reagent/atom false)]
      (fn [m model & [trail]]

        (let [with-no-children (filter (complement has-children?) m)
              with-children    (filter has-children? m)]
          (into [:ul]
                (concat

                  ; Attributes first!
                  (if (and trail @editing?)
                    [:li.haschildren [:p [atts model trail with-no-children]]]
                    (->> with-no-children
                         (map (fn [[k v]]
                                (let [loc (vec (conj trail k))]
                                  [:li.haschildren [:p (uncamel k)]])))))

                  (list (when trail [:i.fa.fa-plus {:on-click (fn [] (swap! editing? not))}]))

                  ; Attributes first!
                  #_(cond-> []
                            (and trail @editing?)
                            (vec (->> with-no-children
                                      (map (fn [[k v]]
                                             (let [loc (vec (conj trail k))]
                                               [:li.haschildren [:p
                                                                 (when @editing?
                                                                   [:i.fa.fa-check-square-o]) (uncamel k)]])))))
                            true (conj [:li.haschildren
                                        [:div
                                         [:p.test123
                                          [:div ;button.btn.btn-primary.il
                                           (when trail
                                             [:i.fa.fa-plus
                                              {:on-click (fn [] (swap! editing? not))}])]
                                          (when (and trail @editing?)
                                            [:div
                                             [atts model trail with-no-children]])]]]))



                  ;(cond-> (vec (->> with-no-children
                  ;                  (map (fn [[k v]]
                  ;                         (let [loc (vec (conj trail k))]
                  ;                           [:li.haschildren [:p
                  ;                                             (when @editing?
                  ;                                               [:i.fa.fa-check-square-o]) (uncamel k)]])))))
                  ;        true (conj [:li.haschildren
                  ;                    [:div
                  ;                     [:p.test123
                  ;                      [:div ;button.btn.btn-primary.il
                  ;                       (when trail
                  ;                         [:i.fa.fa-plus
                  ;                          {:on-click (fn [] (swap! editing? not))}])]
                  ;                      (when (and trail @editing?)
                  ;                        [:div
                  ;                         [atts model trail with-no-children]])]
                  ;                     ]]))

                  ; Then collections!
                  (->> with-children
                       (map (fn [[k v]]
                              (let [loc (vec (conj trail k))]
                                [:li.haschildren
                                 [:p {:class "classy"}
                                  k
                                  [:button.btn.btn-primary.il [:i.fa.fa-share-alt]]]
                                 [:div
                                  [tree v model loc]]]))))))))))