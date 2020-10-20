(ns bluegenes.pages.reportpage.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.pages.reportpage.components.summary :as summary]
            [bluegenes.pages.reportpage.components.table :as report-table]
            [bluegenes.pages.reportpage.components.toc :as toc]
            [bluegenes.pages.reportpage.components.sidebar :as sidebar]
            [bluegenes.components.table :as table]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.tools.views :as tools]
            [bluegenes.pages.reportpage.subs :as subs]
            [im-tables.views.core :as im-table]
            [bluegenes.route :as route]
            [bluegenes.components.viz.views :as viz]))

(defn heading []
  [:h1 "BRCA1"
   [:code.start {:class (str "start-" "Gene")} "Gene"]])

(defn main []
  [:div.container-fluid.report-page
   [:div.row
    [:div.col-xs-2
     [toc/main]]
    [:div.col-xs-7
     [heading]
     [report-table/main]]
    [:div.col-xs-3
     [sidebar/main]]]])
