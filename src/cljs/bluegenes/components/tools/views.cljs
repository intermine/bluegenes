(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.tools.subs :as subs]
            [bluegenes.components.tools.events :as events]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.components.bootstrap :refer [poppable]]))

;; The following component may be used by itself to display one tool.
(defn tool []
  ;; We don't have a nil state so we use an atom to check if the user has
  ;; overridden the layout-provided collapse value.
  (let [override-collapse* (reagent/atom nil)]
    (fn [{{:keys [cljs human]} :names} & {:keys [collapse id]}]
      (let [collapsed? (or @(subscribe [::subs/collapsed-tool? cljs])
                           (and (not @override-collapse*)
                                collapse))
            ;; Most tools have a name starting with "BlueGenes"
            ;; which is frankly not very useful, so we remove it.
            pretty-name (str/replace human #"(?i)^bluegenes\s*" "")]
        ;; Please avoid changing the markup that follows. Its structure is
        ;; copied to each tool's demo.html so they get a similar styling.
        [:div.report-item
         {:class (when collapsed? :report-item-collapsed)
          :id id}
         [:h4.report-item-heading
          {:on-click (fn []
                       (reset! override-collapse* true)
                       (if collapsed?
                         (dispatch [::events/expand-tool cljs])
                         (dispatch [::events/collapse-tool cljs])))}
          [:span.report-item-title
           pretty-name
           [poppable {:data [:span "This is a visualization and may take longer to load. If you click to collapse, it will stay hidden on all pages until you expand it again."]
                      :children [icon "bar-chart"]}]]
          [:span.report-item-toggle
           (if collapsed?
             [icon "expand-folder"]
             [icon "collapse-folder"])]]
         [:div.report-item-tool {:class [cljs (when collapsed? :hidden)]}
          [:div {:id cljs}]]]))))

(defn main []
  (let [suitable-tools @(subscribe [::subs/suitable-tools])]
    [:div
     (for [tool-details suitable-tools]
       ^{:key (get-in tool-details [:names :cljs])}
       [tool tool-details])]))
