(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent :refer [create-class]]
            [imcljs.path :as p]))


(defn one-of? [haystack needle]
  (some? (some #{needle} haystack)))

;(def s (reagent/atom [["Gene" "alleles" "name"]]))
(def s (reagent/atom {"Gene" {"alleles" {"name" true}}}))

(defn tree-view-recur []
  (let [expanded? (reagent/atom false)]
    (fn [model root-class trail selected]
      (let [{:keys [displayName attributes collections references]} (get-in model [:classes root-class])]
        [:ul.qb {:class (if @expanded? "open")}
         [:li {:on-click (fn [] (swap! expanded? not))}
          [:div.nowrap
           (if @expanded?
             [:span.glyphicon.glyphicon-chevron-down]
             [:span.glyphicon.glyphicon-chevron-right])
           [:div.class.nowrap.inlineblock displayName]]]
         (if @expanded?
           (concat
             (map (fn [[_ {:keys [name]}]]
                    (let [selected? (get-in selected (conj trail name))]
                      ^{:key name}
                      [:li {:class    (if selected? "selected")
                            :style    {:padding-left "35px"}
                            :on-click (fn [] (if selected?
                                               (swap! s (fn [m] (update-in m trail dissoc name)))
                                               (swap! s (fn [m] (assoc-in m (conj trail name) true)))))}
                       [:span
                        name
                        [:div.button-group
                         [:button.btn "Show"]
                         [:button.btn "Constrain"]
                         ]]]))
                  attributes)
             (map (fn [[_ colref]]
                    ^{:key (str (name root-class) (:name colref))}
                    [:li [tree-view-recur
                          model
                          (keyword (:referencedType colref))
                          (conj trail (:name colref))
                          selected]])
                  (merge collections references))))]))))

(defn tree-view []
  (fn [model root-class]
    [tree-view-recur
     model
     root-class
     [(name root-class)]
     @s]))

(defn main []
  (let [cm (subscribe [:current-mine])]
    (fn []
      ;(.log js/console "alekd" (p/walk (get-in @cm [:service :model]) "Gene.diseases.publications"))
      [:div.container
       [:div.row
        [:div.col-sm-6
         [tree-view (get-in @cm [:service :model]) :Gene]
         ]
        [:div.col-sm-6
         "I am the query builder"
         [:button.btn.btn-raised
          {:on-click (fn []
                       (.log js/console "Log Value"
                             @s)
                       )}
          "Log Value"]]]])))

