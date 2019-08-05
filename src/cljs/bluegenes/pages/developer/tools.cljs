(ns bluegenes.pages.developer.tools
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [bluegenes.components.tools.subs :as tools-subs]
            [bluegenes.pages.developer.events :as events]
            [markdown-to-hiccup.core :as md]))

(defn action [func]
  (fn [e]
    (.preventDefault e)
    (func)))

(defn form->page
  [form]
  (let [report     "Report page"
        list       "List results page"
        deprecated (str "DEPRECATED: " form)]
    (case form
      "id"
      report
      ("ids" "records" "rows" "query")
      list
      ("list" "lists")
      deprecated)))

(defn output-tool-accepts
  "Show all pages a single tool will be displayed for."
  [accepts]
  [:div.tool-class [:h3 "Supported pages:"]
   (into [:ul]
         (map (fn [form]
                [:li (form->page form)])
              accepts))])

(defn output-tool-classes
  "Show all object type classes a single tool will be displayed for. "
  [classes]
  [:div.tool-class [:h3 "Supported data types:"]
   (into [:ul]
         (map (fn [the-class]
                [:li {:class (str "type-" the-class)} the-class]) classes))])

(defn tool-description
  [text]
  (when (not-empty text)
    [:div.description [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
     (-> text
         md/md->hiccup
         md/component
         (md/hiccup-in :div :p))]))

(defn installed-tool-list
  "Display all tool types to the user."
  [tools]
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
        [tool-description (get-in tool [:package :description])]
        [output-tool-classes (get-in tool [:config :classes])]
        [output-tool-accepts (get-in tool [:config :accepts])]]
       [:div.tool-footer
        [:div.tool-data
         [:span (str "v" (get-in tool [:package :version]))]]
        [:div.tool-actions
         [:button.btn.btn-primary.btn-raised
          {:on-click (action #(dispatch [::events/uninstall-tool
                                         (-> tool :package :name)]))}
          "Remove"]]]])
    @tools)))

(defn available-tool-list
  "Display tools fetched from a keyword search on npm."
  [tools]
  (into
   [:div.tool-list]
   (map
    (fn [{{:keys [name description]} :package}]
      [:div.tool
       [:h2 name]
       [:div.details
        [tool-description description]]
       [:div.tool-footer
        [:div.tool-data
         "Install for more information."]
        [:div.tool-actions
         [:button.btn.btn-primary.btn-raised
          {:on-click (action #(dispatch [::events/install-tool name]))}
          "Install"]]]])
    @tools)))

(defn tool-store
  "Page structure for tool store UI"
  []
  (let [installed-tools (subscribe [::tools-subs/installed-tools])
        remaining-tools (subscribe [::tools-subs/remaining-tools])]
    [:div.tool-store
     [:h1 "Tool Store"]
     [:div
      (when (seq @installed-tools)
        [:div.info
         [:h4 "Installed tools"]
         [:button.btn.btn-primary.btn-raised
          {:on-click (action #(dispatch [::events/update-all-tools]))}
          "Update installed tools"]])
      [installed-tool-list installed-tools]
      (when (seq @remaining-tools)
        [:div.info
         [:h4 "Available tools"]
         [:button.btn.btn-primary.btn-raised
          {:on-click (action #(dispatch [::events/install-all-tools]))}
          "Install all tools"]])
      [available-tool-list remaining-tools]]]))
