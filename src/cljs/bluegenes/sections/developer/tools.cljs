(ns bluegenes.developer.tools
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.events.developer :as events]
            [bluegenes.subs.developer :as subs]
            [accountant.core :refer [navigate!]]))

(def page-types {:report-page "Report page"
                 :list-results-page "List results page"})
(def tool-types {"id" :report-page
                 "ids" :list-results-page
                 "list" :list-results-page
                 "lists" :list-results-page
                 "records" :list-results-page
                 "rows" :list-results-page
                 "query" :list-results-page})

(defn get-tool-types [tool]
  (into [:span]
        (map
         (fn [tool-type]
           (let [the-type (get tool-types tool-type)]
             (get page-types the-type)))
         (get-in tool [:config :accepts]))))

(defn output-tool-classes [classes]
  (into [:div.tool-class [:h3 "Suitable for pages with these classes:"]]
        (map (fn [the-class]
               [:span {:class (str "type-" the-class)} the-class]) classes)))

(defn tool-list []
  (let [tools (subscribe [::subs/tools])]
    (into
     [:div.tool-list]
     (map
      (fn [tool]
        [:div.tool
         [:h2 (get-in tool [:package :name])]
         [:span.tool-type [:h3 "Tool Type:"] [get-tool-types tool]]
         [output-tool-classes (get-in tool [:config :classes])]])
      (:tools @tools)))))

(defn tool-store []
  [:div
   [:h1 "Tool Store"]
   [:div.panel.container
    [:div "Showing all tools that are currently installed for Report Pages"]
    [tool-list]]])
