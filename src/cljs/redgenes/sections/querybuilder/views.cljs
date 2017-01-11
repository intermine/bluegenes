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
      (let [{:keys [displayName attributes collections references]} (get-in model [:classes root-class])]
        [:ul.qb {:class (if (and @expanded? (> (count trail) 1)) "open")}
         [:li {:on-click (fn [] (swap! expanded? not))}
          [:div.nowrap
           (if @expanded?
             [:span.glyphicon.glyphicon-chevron-down]
             [:span.glyphicon.glyphicon-chevron-right])
           [:div.class.nowrap.inlineblock displayName]]]
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
                  attributes)
             ; Combined with a map of collections and references
             (map (fn [[_ colref]]
                    ^{:key (str (name root-class) (:name colref))}
                    [:li [tree-view-recur model (keyword (:referencedType colref)) (conj trail (:name colref)) selected]])
                  (merge collections references))))]))))

(defn tree-view []
  (fn [m model root-class]
    [tree-view-recur model root-class [(name root-class)] m]))

(defn query-view []
  (let [query-constraints (subscribe [:qb/query-constraints])]
    (fn [model m trail]
      (into [:ul.qb]
            (map (fn [[k value]]
                   (let [children?      (map? value)
                         my-constraints (filter (fn [c] (= (join "." (conj trail k)) (:path c))) @query-constraints)]
                     [:li
                      [:div
                       k
                       [:div.button-group
                        [:button.small-btn
                         {:on-click (fn [e]
                                      (ocall e :stopPropagation)
                                      (dispatch [:qb/remove-view (conj trail k)]))}
                         [:i.fa.fa-times]]]
                       (if (not-empty my-constraints)
                         (into [:div {:style {:padding-left "30px"
                                              :display      "block"}}]
                               (map (fn [con]
                                      [:div
                                       {:style {:display "block"}}
                                       [constraint
                                        :model model
                                        :path (:path con)
                                        :value (:value con)
                                        :op (:op con)
                                        :on-change (fn [] (println "CHANGED"))
                                        :label? false]])
                                    my-constraints)))

                       (if children? [query-view model value (if trail (conj trail k) [k])])]]))
                 (into (sorted-map) m))))))




(defn main []
  (let [query-map         (subscribe [:qb/query-map])
        query-constraints (subscribe [:qb/query-constraints])
        cm                (subscribe [:current-mine])]
    (fn []
      [:div.main-window
       [:div.sidex
        [tree-view @query-map (get-in @cm [:service :model]) :Gene]]
       [query-view (get-in @cm [:service :model]) @query-map]])))

