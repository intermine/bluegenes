(ns bluegenes.pages.tools.view
  (:require [re-frame.core :as re-frame :refer [dispatch subscribe]]
            [bluegenes.components.tools.subs :as tools-subs]
            [bluegenes.pages.tools.events :as events]
            [bluegenes.version :as version]
            [bluegenes.components.viz.views :refer [all-viz]]
            [bluegenes.utils :refer [md-paragraph]]
            [bluegenes.config :refer [server-vars]]))

;; You may notice that this page is only linked from the admin's profile
;; dropdown, but there's no guard to stop logged in or anonymous users from
;; accessing it directly by URL. This is intentional, as having it linked for
;; regular users seems misleading, but it still may be useful to access if
;; you're not an admin. It contains no secret information, and no harm can be
;; done as the bluegenes-tool-store backend will verify that you're admin
;; before doing anything.

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

(defn output-tool-depends
  "Shows an alert if the current mine's model does not support this tool."
  [depends]
  (let [model       @(subscribe [:model])
        mine-name   (:name @(subscribe [:current-mine]))
        supported?  (every? #(contains? model %) (map keyword depends))
        unsupported (remove #(contains? model (keyword %)) depends)]
    (when-not supported?
      [:div.tool-alert
       [:p (str "This tool will not show on " mine-name " as it doesn't support: ")
        (into [:<>]
              (interpose " " (map #(vector :code %) unsupported)))
        ". However, it may be shown for other mines."]])))

(defn output-tool-version
  "Shows an alert if the version does not match this Bluegenes' tool API."
  [version]
  (let [supported? (= version version/tool-api)]
    (when-not supported?
      [:div.tool-alert
       [:p "This tool is disabled due to using a different Tool API version than this BlueGenes instance. "
        [:code (str "Tool: " version)]
        " "
        [:code (str "BlueGenes: " version/tool-api)]
        (if (< version version/tool-api)
          " We recommend updating the tool to the latest version."
          " We recommend updating BlueGenes to the latest version.")]])))

(defn tool-description
  [text]
  (when (not-empty text)
    [:div.description
     [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
     (md-paragraph text)]))

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
         [:div.tool-preview
          [:img {:src (str (:bluegenes-deploy-path @server-vars) (:hasimage tool))
                 :height "220px"}]]
         [:div.tool-no-preview "No tool preview available"])
       [:div.details
        [tool-description (get-in tool [:package :description])]
        [output-tool-version (get-in tool [:config :version] 1)]
        [output-tool-depends (get-in tool [:config :depends])]
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

(defn native-viz-list
  "Display all native visualizations integrated into BlueGenes."
  [vizs]
  (into [:div.tool-list]
        (for [{{:keys [accepts classes depends version]} :config :as viz} vizs]
          [:div.tool
           [:h2 (get-in viz [:config :toolName :human])]
           [:div.details
            [tool-description (get-in viz [:package :description])]
            [output-tool-version (or version 1)]
            [output-tool-depends depends]
            [output-tool-classes classes]
            [output-tool-accepts accepts]]
           [:div.tool-footer
            [:div.tool-data
             "Included with BlueGenes"]]])))

(defn main
  "Page structure for tool store UI"
  []
  (let [installed-tools (subscribe [::tools-subs/installed-tools])
        remaining-tools (subscribe [::tools-subs/remaining-tools])]
    (fn []
      [:div.tool-store.container
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
        [available-tool-list remaining-tools]
        (when (seq all-viz)
          [:div.info
           [:h4 "Native visualizations"]])
        [native-viz-list all-viz]]])))
