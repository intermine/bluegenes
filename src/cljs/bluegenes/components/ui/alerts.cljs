(ns bluegenes.components.ui.alerts
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn invalid-token-alert []
  (let [invalid-tokens? (subscribe [:invalid-tokens?])]
    (if @invalid-tokens?
      [:div.alert-container
       [:div.alert.alert-danger
        [:h3 "Debug: Your token has expired"]
        [:p "It's likely that a remote InterMine server restarted and lost your anonymous token. Please refresh your browser to obtain a new one."]
        [:button.btn.btn-default.btn-raised.pull-right
         {:on-click #(dispatch [:reboot])}
         "Refresh"]
        [:div.clearfix]]]
      [:span])))

(defn message
  "A message component that dismisses itself after 5 seconds
  Messages should be in the following format:
  {:markup [:div [:span somemarkup]]
   :style success (or any bootstrap color name)
   }"
  []
  (r/create-class
   {:component-did-mount (fn [this]
                           (let [{:keys [id]} (r/props this)]
                             (js/setTimeout
                              (fn []
                                (dispatch [:messages/remove id])) 5000)))
    :reagent-render (fn [{:keys [markup style when id]}]
                      [:div.alert.message
                       {:class (str "alert-" (or style "info"))}
                       [:span.markup markup]
                       [:span.controls
                        [:button.btn.btn-default.btn-xs.btn-raised
                         {:on-click (fn [] (dispatch [:messages/remove id]))
                          :style {:margin 0}} "X"]]])}))

(defn messages
  "Creates a message bar on the bottom of the screen"
  []
  (let [messages (subscribe [:messages])]
    (fn []
      [:div.messages-wrapper
       (into [:div.messages-container]
             (map (fn [m] [message m]) @messages))])))
