(ns bluegenes.components.tools.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.tools.subs :as subs]
            [bluegenes.components.tools.events :as events]
            [clojure.string :as str]
            [bluegenes.components.icons :refer [icon]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [clean-tool-name]]))

;; The following component may be used by itself to display one tool.
(defn tool [{{:keys [cljs]} :names :as tool-details} & {:keys [collapse id]}]

  ;; We don't have a nil state so we use an atom to check if the user has
  ;; overridden the layout-provided collapse value.
  (let [override-collapse* (reagent/atom nil)
        collapsed-tool? (subscribe [::subs/collapsed-tool? cljs])]

    (when-not (or collapse @collapsed-tool?)
      (dispatch [::events/init-tool tool-details (str id "-" cljs)]))

    (fn [{{:keys [cljs human]} :names :as tool-details} & {:keys [collapse id]}]
      (let [collapsed? (or @collapsed-tool?
                           (and (not @override-collapse*)
                                collapse))]
        ;; Please avoid changing the markup that follows. Its structure is
        ;; copied to each tool's demo.html so they get a similar styling.
        [:div.report-item
         {:class (when collapsed? :report-item-collapsed)
          :id id}
         [:h4.report-item-heading
          {:on-click (fn []
                       ;; Only init the tool if it had been initially collapsed
                       ;; (and therefore not previously initialised).
                       (when (and (or collapse @collapsed-tool?)
                                  (not @override-collapse*))
                         (dispatch [::events/init-tool tool-details (str id "-" cljs)]))
                       (if collapsed?
                         (dispatch [::events/expand-tool cljs])
                         (dispatch [::events/collapse-tool cljs]))
                       (reset! override-collapse* true))}
          [:span.report-item-title
           (clean-tool-name human)
           [poppable {:data [:span "This is a visualization and may take longer to load. If you click to collapse, it will stay hidden on all pages until you expand it again."]
                      :children [icon "bar-chart"]}]]
          [:span.report-item-toggle
           (if collapsed?
             [icon "expand-folder"]
             [icon "collapse-folder"])]]
         [:div.report-item-tool {:class [cljs (when collapsed? :hidden)]}
          [:div {:id (str id "-" cljs)}]]]))))

(defn main []
  (let [suitable-tools @(subscribe [::subs/suitable-tools])]
    [:div
     (for [tool-details suitable-tools]
       ^{:key (get-in tool-details [:names :cljs])}
       [tool tool-details])]))
