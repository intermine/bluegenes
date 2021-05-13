(ns bluegenes.components.ui.list_dropdown
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as reagent]
            [oops.core :refer [oget]]
            [goog.string :as gstring]))

(defn has-text?
  "Returns true if the details vector contains a string"
  [string details]
  (if string
    (re-find (re-pattern (str "(?i)" (gstring/regExpEscape string))) (clojure.string/join " " (map details [:name :description])))
    true))

(defn has-type?
  "Returns true if list is of a certain type"
  [type list]
  (if type
    (= type (keyword (:type list)))
    true))

(defn im-lists
  "Component: a list of intermine lists"
  []
  (fn [& {:keys [lists on-change]}]
    (into [:ul]
          (map (fn [{:keys [name title size]}]
                 [:li
                  {:on-click (partial on-change name)}
                  [:a [:span.list-selection title] [:span.size (str " (" size ")")]]])) lists)))

(defn text-filter-form []
  (fn [text-filter-atom]
    [:form.form
     [:input.form-control
      {:type        "text"
       :value       @text-filter-atom
       :on-change   (fn [e] (reset! text-filter-atom (oget e :target :value)))
       :placeholder "Filter..."}]]))

(defn list-dropdown
  "Creates a dropdown for intermine lists
  :value          The selected value to show in the dropdown
  :lists          A collection of Intermine lists
  :restrict-type  (Optional) a keyword to restrict the list to a type, like :Gene
  :on-change      A function to call with the name of the list"
  []
  (let [text-filter-atom (reagent/atom nil)]
    (fn [& {:keys [value lists restrict-type on-change disabled]}]
      (let [status-filter  #(= "CURRENT" (:status %))
            type-filter    (partial has-type? restrict-type)
            text-filter    (partial has-text? @text-filter-atom)
            suitable-lists (filter (every-pred status-filter type-filter) lists)
            filtered-lists (filter text-filter suitable-lists)]
        [:div.dropdown.list-dropdown
         [:button.btn.btn-raised.btn-default.dropdown-toggle
          {:disabled disabled
           :data-toggle "dropdown"}
          [:span.list-name
           (when value
             {:title value})
           (or value "Choose a list")]
          [:span.caret]]
         [:div.dropdown-menu.dropdown-mixed-content
          (if (seq suitable-lists)
            [:div.container-fluid
             [text-filter-form text-filter-atom]
             (if (seq filtered-lists)
               [:<>
                [:div.col-md-6
                 [:h4 [:svg.icon.icon-history [:use {:xlinkHref "#icon-history"}]] " Recently Created"]
                 [im-lists
                  :lists (take 5 (sort-by :timestamp filtered-lists))
                  :on-change on-change]]
                [:div.col-md-6
                 [:h4 [:svg.icon.icon-sort-alpha-asc [:use {:xlinkHref "#icon-sort-alpha-asc"}]] " All Lists"]
                 [:div.clip-400
                  [im-lists
                   :lists (sort-by :name filtered-lists)
                   :on-change on-change]]]]
               [:div.container-fluid
                [:h4 (str "No suitable lists containing \"" @text-filter-atom "\"")]])]
            [:div.container-fluid
             [:h4 (str "No lists available of type " (some-> restrict-type name))]])]]))))
