(ns bluegenes.components.top-scroll
  (:require [re-frame.core :refer [dispatch]]))

(defn main []
  [:div.top-scroll
   {:on-click #(dispatch [:scroll-to-top])
    :title "Scroll to top"}
   [:span.top-arrow]])
