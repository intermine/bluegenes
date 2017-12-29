(ns bluegenes.sections.reportpage.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.sections.reportpage.components.summary :as summary]
            [bluegenes.components.table :as table]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.sections.reportpage.components.minelinks :as minelinks]
            [bluegenes.sections.reportpage.components.tools :as tools]
            [accountant.core :refer [navigate!]]))


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
         ;;let people know we're loading the report
         [loader (str (:type @params) " Report")]
         ;;main content, once it's available.
         [:div
          [:ol.breadcrumb
           [:li [:a {:href "#/" :on-click #(navigate! "/")} "Home"]]
           [:li [:a {:href "#/search" :on-click #(navigate! "/search")} "Search Results"]]
           [:li.active [:a "Report"]]]
           [tools/main]

          [summary/main (:summary @report)]
           (cond (= "Gene" (:type @params))
           [minelinks/main (:id @params)])
          (into [:div.collections] (map (fn [query] [lighttable/main query {:title true}]) @collections))
          (into [:div.templates] (map (fn [[id details]] [table/main details]) @templates))])])))
