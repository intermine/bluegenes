(ns bluegenes.components.ui.alerts
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn tool-operation-alert []
  (let [working? @(subscribe [:bluegenes.pages.tools.subs/tool-working?])]
    (when working?
      [:div.alert-container
       [:div.alert.alert-info
        [:span "A tool operation is in progress..."]]])))

(defn invalid-token-alert []
  (let [invalid-token? (subscribe [:invalid-token?])]
    (if @invalid-token?
      [:div.alert-container
       [:div.alert.alert-danger
        [:h3 "Your token has expired"]
        [:p "You have either been idle for a long time, or the remote InterMine server restarted and lost your authentication token. Click the " [:strong "refresh"] " button below to get a new token. If you were logged in, you will have to login again."]
        [:button.btn.btn-default.btn-raised.pull-right
         {:on-click #(dispatch [:clear-invalid-token])}
         "Refresh"]
        [:div.clearfix]]]
      [:span])))

(defn message
  "A message component that dismisses itself after 5 seconds
  Messages should be in the following format:
      {:markup [:span \"Your text\"]] ; Or any other markup.
       ;; :markup can also be a function which takes `id` and returns hiccup.
       :style \"success\" ; Or any bootstrap color name.
       :timeout 0 ; Optional to override default auto-dismissal of 5 seconds.
       }"
  [{:keys [markup style id]}]
  [:div.alert.message
   {:class (str "alert-" (or style "info"))}
   [:span.markup (cond-> markup (fn? markup) (apply [id]))]
   [:span.controls
    [:button.btn.btn-default.btn-xs.btn-raised
     {:on-click (fn [] (dispatch [:messages/remove id]))
      :style {:margin 0}} "X"]]])

(defn messages
  "Creates a message bar on the bottom of the screen"
  []
  (let [messages (subscribe [:messages])]
    (fn []
      [:div.messages-wrapper
       (into [:div.messages-container]
             (for [{:keys [id] :as m} @messages]
               ^{:key id}
               [message m]))])))

(defn main
  []
  [:<> ; This hieroglyph is read as "React Fragment".
   ;; It lets you return multiple elements without wrapping them in a container element.
   [tool-operation-alert]
   [invalid-token-alert]
   [messages]])
