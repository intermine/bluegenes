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




(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:title details)]
      (re-find (re-pattern (str "(?i)" string)) (clojure.string/join " " (map details [:name :description])))
      false)
    true))

(defn list-dropdown []
  (let [lists       (subscribe [:lists])
        listskw (subscribe [:listskw])
        filter-text (reagent/atom nil)
        current-mine-name (subscribe [:current-mine-name])]
    (fn []
      (let [lists (filter (partial has-text? @filter-text) (@current-mine-name @lists))]
        [:div.dropdown
         [:button.btn.btn-default.btn-raised.dropdown-toggle
          {:data-toggle "dropdown"}
          [:span "Lists " [:span.caret]]]

         [:div.dropdown-menu.dropdown-mixed-content
          [:div.container-fluid

           [:form.form
            [:input.form-control
             {:type        "text"
              :value       @filter-text
              :on-change   (fn [e] (reset! filter-text (oget e :target :value)))
              :placeholder "Filter..."}]]

           (if (empty? @filter-text)
             [:div.row
              [:div.col-sm-6
               [:h4 [:i.fa.fa-clock-o] " Recently Created"]
               (into [:ul] (map (fn [{:keys [name size]}] [:li
                                                           [:a
                                                            [:span name]
                                                            [:span.size (str " (" size ")")]]])
                                (take 5 (sort-by :timestamp lists))))

               [:div.sep {:style {:border-bottom "1px solid #dedede"}}]

               [:h4 [:i.fa.fa-clock-o] " Recently Used"]
               (into [:ul] (map (fn [{:keys [name size]}]
                                  [:li
                                   [:a [:span name]
                                    [:span.size (str " (" size ")")]]])
                                (take 5 lists)))]
              [:div.col-sm-6

               [:h4 [:i.fa.fa-sort-alpha-asc] " All Lists"]
               (into [:ul.clip-400] (map (fn [{:keys [name size]}]
                                           [:li
                                            [:a
                                             [:span name]
                                             [:span.size (str " (" size ")")]]])
                                         (sort-by :name lists)))]]
             [:div.col-sm-12
              [:h4 "Filtered..."]
              (into [:ul] (map (fn [{:keys [name size]}]
                                 [:li
                                  [:a
                                   [:span name]
                                   [:span.size (str " (" size ")")]]])
                               (sort-by :name lists)))])]]]))))