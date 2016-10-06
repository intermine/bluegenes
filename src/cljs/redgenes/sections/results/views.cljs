(ns redgenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [redgenes.components.table :as table]
            [redgenes.sections.results.events]
            [redgenes.sections.results.subs]
            [clojure.string :refer [split]]))

(defn main []
  (let [query (subscribe [:results/query])]
    (fn []
      [:div.container-fluid
       ;[:div.row [:h1 (str "Results for " (:title @query))]]
       [:button.btn.btn-primary.btn-raised
        {:on-click (fn []
                     (dispatch
                       [:save-data {:sd/type  :query
                                    :sd/service :flymine
                                    :sd/label (last (split (:title @query) "-->"))
                                    :sd/value (assoc @query :title (last (split (:title @query) "-->")))}]))} "Save"]
       (if @query [table/main @query true])])))