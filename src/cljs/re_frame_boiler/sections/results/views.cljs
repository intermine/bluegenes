(ns re-frame-boiler.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [re-frame-boiler.components.table :as table]
            [re-frame-boiler.sections.results.events]
            [re-frame-boiler.sections.results.subs]))

(defn main []
  (let [query (subscribe [:results/query])]
    (fn []
      [:div.container-fluid
       [:div.row [:h1 "Results"]]
       [:button.btn.btn-primary.btn-raised
        {:on-click (fn []
                     (dispatch
                       [:save-data {:type  :query
                                    :label "My Saved world"
                                    :value @query}]))} "Save"]
       (if @query [table/main @query true])])))