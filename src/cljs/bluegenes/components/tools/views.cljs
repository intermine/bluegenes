(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [bluegenes.components.tools.subs :as subs]
            [bluegenes.components.tools.events :as events]))

(defn main []
  (let [suitable-tools @(subscribe [::subs/suitable-tools])]
    (into [:div.tools]
          (doall
            (for [{{:keys [cljs human]} :names} suitable-tools
                  :let [collapsed? @(subscribe [::subs/collapsed-tool? cljs])]]
              [:div.tool-container
               [:div.tool-header
                [:h3.tool-title human]
                [:button.btn.tool-toggle.pull-right
                 {:on-click (if collapsed?
                              #(dispatch [::events/expand-tool cljs])
                              #(dispatch [::events/collapse-tool cljs]))}
                 (if collapsed? "＋" "—")]]
               [:div.tool {:id cljs :class [cljs (when collapsed? :hidden)]}]])))))
