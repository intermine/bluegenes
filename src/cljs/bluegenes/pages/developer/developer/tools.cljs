(ns bluegenes.pages.developer.tools
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.pages.developer.events :as events]
            [bluegenes.pages.developer.subs :as subs]
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

(defn get-tool-types
  "Check what classes pages a given tool will be able to display on. Currently somewhat defunct, given that we only have report page tools, but will come in handy when we extend to the list results page."
  [tool]
  (into [:span]
        (map
         (fn [tool-type]
           (let [the-type (get tool-types tool-type)]
             (get page-types the-type)))
         (get-in tool [:config :accepts]))))

(defn output-tool-classes
  "Show all object type classes a single tool will be displayed for. "
  [classes]
  [:div.tool-class [:h3 "This tool will display on the following page types:"]
   (into [:ul]
         (map (fn [the-class]
                [:li {:class (str "type-" the-class)} the-class]) classes))])

(defn tool-list
  "Display all tool types to the user."
  []
  (let [tools (subscribe [::subs/tools])]
    (into
     [:div.tool-list]
     (map
      (fn [tool]
        [:div.tool
         [:h2 (get-in tool [:names :human])]
         (if (:hasimage tool)
           [:div.tool-preview [:img {:src (:hasimage tool) :height "220px"}]]
           [:div.tool-no-preview "No tool preview available"])
         [:div.details
          [:div.description [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]] (get-in tool [:package :description])]
          [output-tool-classes (get-in tool [:config :classes])]]])
      (:tools @tools)))))

(defn tool-store
  "Page structure for tool store UI"
  []
  [:div.tool-store
   [:h1 "Tool Store"]
   [:div
    [:div.info
     [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
     [:p "Showing all tools that are currently installed for Report Pages. To add more tools, see the "
      [:a {:href "https://github.com/intermine/bluegenes/tree/dev/docs"} "BlueGenes Documentation"] "."]]
    [tool-list]]])
