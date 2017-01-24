(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]
            [clojure.string :refer [split join]]
            [oops.core :refer [ocall]]
            [redgenes.components.ui.constraint :refer [constraint]]))

(defn one-of? [haystack needle]
  (some? (some #{needle} haystack)))

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
                            :style {:padding-left "35px"}
                            ;:on-click (fn [] (if selected?
                            ;                   (dispatch [:qb/remove-view (conj trail name)])
                            ;                   (dispatch [:qb/add-view (conj trail name)])))
                            }
                       [:span
                        name
                        [:div.button-group
                         (if selected?
                           [:button.small-btn {:on-click (fn [] (dispatch [:qb/remove-view (conj trail name)]))} "Hide"]
                           [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-view (conj trail name)]))} "Show"])
                         [:button.small-btn {:on-click (fn [] (dispatch [:qb/add-constraint (conj trail name)]))} "Constrain"]
                         ]]]))
                  (sort attributes))
             ; Combined with a map of collections and references
             (map (fn [[_ colref]]
                    ^{:key (str (name root-class) (:name colref))}
                    [:li [tree-view-recur model (keyword (:referencedType colref)) (conj trail (:name colref)) selected]])
                  (into (sorted-map) (merge collections references)))))]))))

(defn tree-view []
  (fn [m model root-class]
    [tree-view-recur model root-class [(name root-class)] m]))





(defn query-view-new []
  (fn [model m trail]
    (into [:div.qbcontainer]
          (->> (into (sorted-map) m) ; Sort our query-map alphabetically
               (map (fn [[k {:keys [visible children constraints] :as value}]]
                      (let [path (if trail (conj trail k) [k])]
                        [:div.qbrow
                         [:div
                          [:div.qrow

                           [:div.button-group {:style {:white-space "nowrap"}}

                            ; Don't show the "visible" icon if this path is a class
                            (cond
                              (and (not (p/class? model (join "." path)))
                                   (not-empty constraints))
                              [:button.small-btn
                               {:class    nil ;(if visible "visible")
                                :on-click (fn [e] (ocall e :stopPropagation) (dispatch [:qb/toggle-view path]))}
                               (if visible [:i.fa.fa-eye] [:i.fa.fa-eye-slash])]
                              (and (not (p/class? model (join "." path)))
                                   (empty? constraints))
                              [:span.small-btn.empty
                               [:i.fa.fa-eye]]
                              )

                            [:span.key {:class (if visible "attribute")} k
                             (if-not (empty? trail) ; Don't allow user to drop the root node
                               [:button.small-btn
                                {:on-click (fn [e] (ocall e :stopPropagation) (dispatch [:qb/remove-view path]))}
                                [:i.fa.fa-times]])]

                            ; Provide an "X" icon to remove the view / constraint
                            ]


                           (if constraints
                             (do
                               (into [:div.ts]
                                     (map-indexed (fn [idx con]
                                                    [:div.constraint-row

                                                     [constraint
                                                      :model model
                                                      :path (join "." path)
                                                      :value (:value con)
                                                      :op (:op con)
                                                      :on-change (fn [c] (dispatch [:qb/update-constraint path idx c]))
                                                      :label? false]

                                                     [:button.small-btn.danger
                                                      {:on-click (fn []  (dispatch [:qb/remove-constraint path idx]) )}
                                                      [:i.fa.fa-times]]
                                                     ])
                                                  constraints))))]]
                         (if children [:div.qbcol [query-view-new model children path]])])))))))



(defn main []
  (let [query-map (subscribe [:qb/query-map])
        qm        (subscribe [:qb/qm])
        cm        (subscribe [:current-mine])]
    (fn []
      [:div.main-window
       [:div.sidex
        [tree-view @query-map (get-in @cm [:service :model]) :Gene]]
       ;[query-view (get-in @cm [:service :model]) @query-map]
       [query-view-new (get-in @cm [:service :model]) @qm]

       [:button.btn.btn-success
        {:on-click (fn [] (.log js/console "map" @qm))}
        "Log Query Map"]
       [:button.btn.btn-success
        {:on-click (fn [] (dispatch [:qb/make-query]))}
        "Make Query"]

       #_[:div.xdisplayer
        [:div.xrows
         [:div.xrow
          [:div.xcontrols "controls"]
          [:div.xconstraints [:span [:span "banana"] [:span "c1"]] [:span "c2"]]]]]
       ])))


