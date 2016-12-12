(ns redgenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.results.events]
            [redgenes.sections.results.subs]
            [redgenes.components.enrichment.views :as enrichment]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]
            [json-html.core :as json-html]
            [im-tables.views.core :as tables]))

(defn adjust-str-to-length [length string]
  (if (< length (count string)) (str (clojure.string/join (take (- length 3) string)) "...") string))

(defn breadcrumb []
  (let [history       (subscribe [:results/history])
        history-index (subscribe [:results/history-index])]
    (fn []
      [:div.breadcrumb-container
       [:i.fa.fa-clock-o]
       (into [:ul.breadcrumb.inline]
             (map-indexed
               (fn [idx {{title :title} :value}]
                 (let [adjusted-title (if (not= idx @history-index) (adjust-str-to-length 20 title) title)]
                   [:li {:class (if (= @history-index idx) "active")}
                    [tooltip
                     [:a
                      {:data-placement "bottom"
                       :title          title
                       :on-click       (fn [x] (dispatch [:results/load-from-history idx]))} adjusted-title]]])) @history))])))


(defn main []
    (fn []
      [:div.container.results
       [breadcrumb]
       [:div.row
        [:div.col-md-8.col-sm-12
         [:div.panel.panel-default
          [:div.panel-body
           [tables/main [:results :fortable]]
           ]]]
        [:div.col-md-4.col-sm-12
         [enrichment/enrich]
         ]]
       ]))
