(ns bluegenes.components.ui.alerts
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(defn invalid-token-alert []
  (let [invalid-tokens? (subscribe [:invalid-tokens?])]
    (fn []
      (if @invalid-tokens?
        [:div.alert-container
         [:div.alert.alert-danger
          [:h3 "Debug: Your token has expired"]
          [:p "It's likely that a remote InterMine server restarted and lost your anonymous token. Please refresh your browser to obtain a new one."]
          [:button.btn.btn-default.btn-raised.pull-right
           {:on-click (fn [] (dispatch-sync [:boot]))} "Refresh"]
          [:div.clearfix]]]
        [:span]))))