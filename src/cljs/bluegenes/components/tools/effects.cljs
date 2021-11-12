(ns bluegenes.components.tools.effects
  (:require [re-frame.core :as rf :refer [dispatch reg-fx]]
            [oops.core :refer [ocall+ oset!]]
            [bluegenes.components.tools.events :as events]
            [bluegenes.route :as route]
            [bluegenes.utils :refer [suitable-entities]]
            [clojure.string :as str]
            [bluegenes.config :refer [server-vars]]))

(defmulti navigate
  "Can be used from JS like this:
      navigate('report', {type: 'Gene', id: 1018204}, 'humanmine');
      navigate('query', myQueryObj, 'flymine');
      navigate('list', 'PL_GenomicsEngland_GenePanel:Radial_dysplasia');
  Note that the third argument specifying the mine namespace is optional."
  (fn [target data mine] (keyword target)))

(defmethod navigate :report [_ report-data mine]
  (let [{:keys [type id]} (js->clj report-data :keywordize-keys true)
        source (keyword mine)]
    (dispatch [::route/navigate ::route/report {:type type, :id id, :mine source}])))

(defmethod navigate :query [_ query-data mine]
  (let [query (js->clj query-data :keywordize-keys true)
        source (keyword mine)]
    (dispatch [::events/navigate-query query source])))

(defmethod navigate :list [_ list-name mine]
  (dispatch [::route/navigate ::route/results {:title list-name :mine (keyword mine)}]))

(defn navigate!
  "JS can't call `navigate` if we pass it the multimethod directly, so wrap it!"
  [target data mine]
  (navigate target data mine))

(defn run-script!
  "Executes a tool-api compliant main method to initialise a tool"
  [tool tool-id & {:keys [service entity tries]}]
  ;;the default method signature is
  ;;package-name(el, service, package, state, config, navigate)
  (let [el (.getElementById js/document tool-id)
        ;; We are passing the model as well here in `service`, which is quite
        ;; large, but could probably be useful?
        service (clj->js service)
        package (clj->js entity)
        state (clj->js {})
        config (clj->js (:config tool))
        main (str (get-in tool [:names :cljs]) ".main")]
    ;; It may occur that the element isn't mounted in the DOM when this runs.
    ;; In this case, try again later!
    (when (and (nil? el) (< (or tries 0) 5))
      (.setTimeout js/window
                   #(run-script! tool tool-id :service service :entity entity
                                 :tries ((fnil inc 0) tries))
                   1000))
    (when el
      ;; If we don't wrap in a try-catch, errors thrown from tools can cause
      ;; Bluegenes to halt execution.
      (try
        (ocall+ js/window main el service package state config navigate!)
        (catch js/Error e
          (.error js/console e))))))

(defn fetch-script!
  ;; inspired by https://stackoverflow.com/a/31374433/1542891
  "Dynamically inserts the tool api script into the head of the document.
  If the script element is already present, re-run the tool's main function."
  [tool tool-id & {:keys [service entity]}]
  (let [script-id (str "script-" (get-in tool [:names :cljs]))
        script-elem (.getElementById js/document script-id)]
    ;; Script has been loaded before; re-run the main function.
    (when script-elem
      (run-script! tool tool-id :service service :entity entity))
    ;; Do not add the script tag if it's already there.
    (when-not script-elem
      (let [script-tag (.createElement js/document "script")
            head (aget (.getElementsByTagName js/document "head") 0)
            tool-path (get-in tool [:config :files :js])]
        (when-not tool-path
          (.error js/console "%cTool API: No script path provided for %s" "background:#ccc;border-bottom:solid 3px indianred; border-radius:2px;" (get-in tool [:names :npm])))
        (when tool-path
          (oset! script-tag "id" script-id)
          ;;fetch script from bluegenes-tool-store backend
          (oset! script-tag "src" (str (:bluegenes-deploy-path @server-vars)
                                       "/tools/" (get-in tool [:names :npm])
                                       "/" tool-path
                                       "?v=" (get-in tool [:package :version])))
          ;;run-script will automatically be triggered when the script loads
          (oset! script-tag "onload" #(run-script! tool tool-id
                                                   :service service
                                                   :entity entity))
          ;;append script to dom
          (.appendChild head script-tag))))))

(defn fetch-styles!
  "If the tool api script has a stylesheet as well, load it and insert into the doc"
  [tool _tool-id]
  (let [style-id (str "style-" (get-in tool [:names :cljs]))
        style-elem (.getElementById js/document style-id)]
    ;; Do not add the style tag if it's already there.
    (when-not style-elem
      (let [style-tag (.createElement js/document "link")
            head (aget (.getElementsByTagName js/document "head") 0)
            style-path (get-in tool [:config :files :css])]
        (when style-path
          ;;fetch stylesheet and set some properties
          (oset! style-tag "id" style-id)
          (oset! style-tag "href" (str (:bluegenes-deploy-path @server-vars)
                                       "/tools/" (get-in tool [:names :npm])
                                       "/" style-path
                                       "?v=" (get-in tool [:package :version])))
          (oset! style-tag "type" "text/css")
          (oset! style-tag "rel" "stylesheet")
          ;;append to dom
          (.appendChild head style-tag))))))

;; This effect should be used to invoke the above functions. It can be run
;; both when initially loading the tools and when the input data has changed,
;; to pass the new entity to the tools (eg. opening a different report or
;; results page, or modifying the contents of the result page's im-table).
;; In the latter case, it will merely call the tool's main function.
(reg-fx
 :load-tool
 (fn [{:keys [tool tool-id service]}]
   ;; `entity` is nil if tool is not suitable to be displayed.
   ;; Although this shouldn't be called if that's the case.
   (when-let [entity (:entity tool)]
     (fetch-script! tool tool-id :service service :entity entity)
     (fetch-styles! tool tool-id))))
