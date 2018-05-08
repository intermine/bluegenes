(ns bluegenes.sections.reportpage.components.tools
  (:require [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [ocall+ oapply oget oget+ oset!]]
            [bluegenes.sections.reportpage.subs :as subs]))

;;fixes the inability to iterate over html vector-like things
(extend-type js/HTMLCollection
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn create-package []
  (let [package-details (subscribe [:panel-params])
        package {:class (:type @package-details)
        ;;we'll need to reformat deep links if we want this to not be hardcoded.
                 :format "id"
                 :value (:id @package-details)}]
    (clj->js package)))

(defn run-script [tool tool-id]
  ;;the default method signature is
  ;;package-name(el, service, package, state, config)
  (let [el (.getElementById js/document tool-id)
        service (clj->js (:service @(subscribe [:current-mine])))
        package (create-package)
        config (clj->js (:config tool))]
         (ocall+ js/window (str (:name tool) ".main") el service package nil config)
  ))

(defn fetch-script
  ;; inspired by https://stackoverflow.com/a/31374433/1542891
  ;; I don't much like fetching the script in the view, but given
  ;; that this is heavy dom manipulation it seems necessary.
  [tool tool-id]
  (let [script-tag (.createElement js/document "script")
        head (first (.getElementsByTagName js/document "head"))]
    ;;fetch script
    (oset! script-tag "src" (str "/tools/" (:name tool) "/dist/bundle.js"))
    ;;run-script will automatically be triggered when the script loads
    (oset! script-tag "onload" #(run-script tool tool-id))
    ;;append script to dom
    (.appendChild head script-tag)))

(defn main []
  (let [toolses           (subscribe [::subs/tools-by-current-type])]
    (into [:div.tools]
          (map
           (fn [tool]
             (let [tool-id (gensym (:name tool))]
               (fetch-script tool tool-id)
               [:div.tool
                [:h3 (:name tool)]
                [:div {:id tool-id}]]))
           @toolses))))
