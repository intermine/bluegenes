(ns redgenes.sections.objects.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [redgenes.sections.objects.components.summary :as summary]
            [redgenes.components.table :as table]
            [redgenes.components.collection :as collection]
            [redgenes.components.lighttable :as lighttable]
            [redgenes.sections.objects.components.minelinks :as minelinks]
            [accountant.core :refer [navigate!]]
))

(defn main []
  (let [params           (subscribe [:panel-params])
        report           (subscribe [:report])
        categories       (subscribe [:template-chooser-categories])
        templates        (subscribe [:runnable-templates])
        collections      (subscribe [:collections])
        fetching-report? (subscribe [:fetching-report?])]
    (fn []
      [:div.container-fluid.report
       (if @fetching-report?
         [:i.fa.fa-cog.fa-spin.fa-3x.fa-fw]
         [:div
          [:ol.breadcrumb
           [:li [:a "Home"]]
           [:li [:a {:href "#/search" :on-click #(navigate! "#/search")} "Search Results"]]
           [:li.active [:a "Report"]]]
          [summary/main (:summary @report)]
           (cond (= "Gene" (:type @params))
           [minelinks/main (:id @params)])
          (into [:div.collections] (map (fn [query] [lighttable/main query {:title true}]) @collections))
          (into [:div.templates] (map (fn [[id details]] [table/main details]) @templates))])])))
