(ns re-frame-boiler.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.sections.objects.components.summary :as summary]))

(defn main []
  (let [params     (subscribe [:panel-params])
        report     (subscribe [:report])
        categories (subscribe [:template-chooser-categories])]
    (fn []
      [:div#wrapper
       [:div#sidebar-wrapper
        (into [:ul.sidebar-nav
               [:li.sidebar-brand
                [:a "Categoriess"]]]
              (map (fn [cat] [:li [:a cat]]) @categories))]
       [:div#page-content-wrapper
        [summary/main (:summary @report)]]])))