(ns bluegenes.developer.tools
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.events.developer :as events]
            [bluegenes.subs.developer :as subs]
            [accountant.core :refer [navigate!]]))

(defn tool-store []

  [:div
   [:h1 "Tool Store"]
   [:div.panel.container
    (let [tools (subscribe [::subs/tools])]
       (.log js/console "%c@tools" "color:mediumorchid;font-weight:bold;" (clj->js @tools))
       (into [:div] (map (fn [tool]
                           [:div.tool
                            [:h4 (get-in tool [:package :name])]
                            [:dl
                            [:dt "Accepts" ] [:dd (clojure.string/join ", " (get-in tool [:config :accepts]))]
                            [:dt "Classes" ] [:dd (clojure.string/join ", " (get-in tool [:config :classes]))]
                             ]
                            ])  (:tools @tools)))

      )
    ]])
