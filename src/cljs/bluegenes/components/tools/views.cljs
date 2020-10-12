(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.components.tools.subs :as subs]
            [bluegenes.components.tools.events :as events]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]))

(defn main []
  (let [suitable-tools @(subscribe [::subs/suitable-tools])]
    (into [:div.tools]
          (doall
            (for [{{:keys [cljs human]} :names} suitable-tools
                  :let [collapsed? @(subscribe [::subs/collapsed-tool? cljs])
                        ;; Most tools have a name starting with "BlueGenes"
                        ;; which is frankly not very useful, so we remove it.
                        pretty-name (str/replace human #"(?i)^bluegenes\s*" "")]]
              ;; Please do not change the markup that follows. Its structure is
              ;; copied to each tool's demo.html so they get a similar styling.
              ;; However, you are free to change the contents of div.tool-header
              [:div.tool-container
               [:div.tool-header
                [:h4.tool-title pretty-name]
                [:button.btn.btn-link.tool-toggle
                 {:on-click (if collapsed?
                              #(dispatch [::events/expand-tool cljs])
                              #(dispatch [::events/collapse-tool cljs]))}
                 (if collapsed?
                   [icon "expand-folder"]
                   [icon "collapse-folder"])]]
               [:div.tool {:class [cljs (when collapsed? :hidden)]}
                [:div {:id cljs}]]])))))
