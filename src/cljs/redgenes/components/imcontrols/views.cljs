(ns redgenes.components.imcontrols.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget]]))



"Creates a dropdown of known organisms. The supplied :on-change function will
receive all attributes of the organism selected.
Options {}:
  :label (optional) Supply this if you want to change the dropdown's label
  :on-change Function to call when changed
Example usage:
  [im-controls/organism-dropdown
   {:label     (if-let [sn (get-in @app-db [:my-tool :selected-organism :shortName])]
                 sn \"All Organisms\")
    :on-change (fn [organism]
                 (dispatch [:mytool/set-selected-organism organism]))}]
"
(defn organism-dropdown []
  (let [organisms (subscribe [:cache/organisms])]
    (fn [{:keys [label on-change]}]
      [:div.btn-group
       [:button.btn.btn-primary.dropdown-toggle
        {:data-toggle "dropdown"}
        [:span (if label (str label " ") "All Organism ") [:span.caret]]]
       (-> [:ul.dropdown-menu]
           (into [[:li [:a {:on-click (partial on-change nil)}
                        [:span [:i.fa.fa-times] " Clear"]]]
                  [:li.divider]])
           (into (map (fn [organism]
                        [:li [:a
                              {:on-click (partial on-change organism)}
                              (:shortName organism)]])
                      (sort-by :shortName @organisms))))])))
