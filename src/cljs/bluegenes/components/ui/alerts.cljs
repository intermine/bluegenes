(ns bluegenes.components.ui.alerts
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn tool-operation-alert []
  (let [working? @(subscribe [:bluegenes.pages.developer.subs/tool-working?])]
    (when working?
      [:div.alert-container
       [:div.alert.alert-info
        [:span "A tool operation is in progress..."]]])))

(defn invalid-token-alert []
  (let [invalid-token? (subscribe [:invalid-token?])]
    (if @invalid-token?
      [:div.alert-container
       [:div.alert.alert-danger
        [:h3 "Debug: Your token has expired"]
        [:p "It's likely that a remote InterMine server restarted and lost your anonymous token. Please refresh your browser to obtain a new one."]
        [:button.btn.btn-default.btn-raised.pull-right
         {:on-click #(dispatch [:clear-invalid-token])}
         "Refresh"]
        [:div.clearfix]]]
      [:span])))

(defn message
  "A message component that dismisses itself after 5 seconds
  Messages should be in the following format:
      {:markup [:span \"Your text\"]] ; Or any other markup.
       :style \"success\" ; Or any bootstrap color name.
       :timeout 0 ; Optional to override default auto-dismissal of 5 seconds.
       }"
  []
  (r/create-class
   {:component-did-mount
    (fn [this]
      (let [{:keys [id timeout] :or {timeout 5000}} (r/props this)]
        (when (and timeout (pos? timeout))
          (js/setTimeout
           #(dispatch [:messages/remove id])
           timeout))))
    :reagent-render
    (fn [{:keys [markup style id]}]
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

(defn main
  []
  [:<> ; This hieroglyph is read as "React Fragment".
   ;; It lets you return multiple elements without wrapping them in a container element.
   [tool-operation-alert]
   [invalid-token-alert]
   [messages]])
