(ns redgenes.sections.querybuilder.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.path :as p]))


(defn tree-view []
  (let [expanded? (reagent/atom false)]
    (fn [root-class model]
      (let [{:keys [displayName attributes collections references]} (get-in model [:classes root-class])]
        [:ul.qb
         [:li {:on-click (fn [] (swap! expanded? not))}
          [:div.nowrap
           [:span.glyphicon.glyphicon-chevron-right]
           [:div.class.nowrap.inlineblock displayName]]]
         (if @expanded?
           (concat (map (fn [[_ {:keys [name]}]]
                         ^{:key name} [:li name])
                       collections)
                  (map (fn [[_ colref]]
                         (println "KEY" (str (name root-class) (:name colref)))
                         ^{:key (str (name root-class) (:name colref))} [:li [tree-view
                                                                              (keyword (:referencedType colref))
                                                                              model]])
                       (merge collections references))))]))))

(defn main []
  (let [cm (subscribe [:current-mine])]
    (fn []
      (.log js/console "alekd" (p/walk (get-in @cm [:service :model]) "Gene.diseases.publications"))
      [:div.container
       [:div.row
        [:div.col-sm-6
         [tree-view :Gene (get-in @cm [:service :model])]
         ]
        [:div.col-sm-6
         "I am the query builder"]]])))

