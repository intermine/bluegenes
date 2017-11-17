(ns bluegenes.components.toast
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))

(defn toast-item []
  (fn [message]
    [:div.toast-item (str message)]))

(defn main []
  (let [toasts (subscribe [:toasts])]
    (fn []
      (into [:div.toast-container] (map (fn [t] [toast-item t]) @toasts)))))