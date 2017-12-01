(ns bluegenes.developer.tools
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.events.developer :as events]
            [bluegenes.subs.developer :as subs]
            [accountant.core :refer [navigate!]]))

(defn tool-store []

  [:div
   [:h1 "Tool Store"]
   [:div.panel.container]])
