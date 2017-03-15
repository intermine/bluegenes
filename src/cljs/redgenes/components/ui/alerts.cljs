(ns redgenes.components.ui.alerts
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(defn invalid-token-alert []
  (let [invalid-tokens? (subscribe [:invalid-tokens?])]
    (fn []
      (if @invalid-tokens?
        [:div.alert-container
         [:div.alert.alert-danger
          [:h3 "Your session has expired"]
          [:button.btn.btn-default.btn-raised
           {:on-click (fn [] (dispatch-sync [:boot]))} "Restart"]]]
        [:span]))))