(ns re-frame-boiler.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [re-frame-boiler.sections.objects.components.summary :as summary]))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])]
    (fn []
      [:div#wrapper
       [:div#sidebar-wrapper
        [:ul.sidebar-nav
         [:li.sidebar-brand
          [:a "Categories"]]
         [:li [:a "Category One"]]]]
       [:div#page-content-wrapper
        [summary/main (:summary @report)]]])))