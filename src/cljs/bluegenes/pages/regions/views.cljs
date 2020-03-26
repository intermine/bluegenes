(ns bluegenes.pages.regions.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [bluegenes.pages.regions.graphs :as graphs]
            [bluegenes.components.table :as table]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.pages.regions.events]
            [bluegenes.pages.regions.subs]
            [bluegenes.pages.regions.results :refer [results-section]]
            [bluegenes.components.imcontrols.views :as im-controls]
            [bluegenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget ocall]]))

(def css-transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransitionGroup))

(defn ex
  "Generate example region search input based on pre-configured per-mine settings"
  []
  (let [active-mine (subscribe [:current-mine])
        example-text (:regionsearch-example @active-mine)]
    ;; We can't use split-lines because the lines are split with an escaped
    ;; slash - that is, they are split on `\\n` and not `\n`
    (clojure.string/join "\n" (split example-text #"\\n"))))

(def region-help-content-popover ;;help text
  (str "Genome regions in the following formats are accepted:"
       "\n - chromosome:start..end, e.g. 2L:11334..12296"
       "\n - chromosome:start-end, e.g. 2R:5866746-5868284 or chrII:14646344-14667746"
       "\n - tab delimited"))

(defn non-empty-classes
  "Given an intermine model (or submodel), only keep classes that have data"
  [model]
  (->> model
       (filter (comp pos? :count second))
       (sort-by (comp :displayName second))))

(defn feature-branch
  "Recursively building a tree of user-selectable features as checkboxes"
  []
  (let [settings (subscribe [:regions/settings])]
    (fn [[class-kw {:keys [displayName descendants count] :as n}]]
      [:li {:class (if (class-kw (:feature-types @settings)) "selected")
            :on-click (fn [e]
                        (.stopPropagation e)
                        (dispatch [:regions/toggle-feature-type n]))}
       (if (class-kw (:feature-types @settings))
         [:svg.icon.icon-checkbox-checked [:use {:xlinkHref "#icon-checkbox-checked"}]]
         [:svg.icon.icon-checkbox-unchecked [:use {:xlinkHref "#icon-checkbox-unchecked"}]])
       (str displayName)
       (when-not (empty? descendants)
         (into [:ul.features-tree]
               (map (fn [d] [feature-branch d]) (non-empty-classes descendants))))])))

(defn feature-types-tree []
  "UI component for checkbox features selection"
  (let [known-feature-types (subscribe [:regions/sequence-feature-types])
        settings (subscribe [:regions/settings])]
    (fn []
      (into [:ul.features-tree]
            (map (fn [f] [feature-branch f]) (non-empty-classes @known-feature-types))))))

(defn organism-selection
  "UI component allowing user to choose which organisms to search. Defaults to all."
  []
  (let [settings (subscribe [:regions/settings])]
    [:div [:label "Organism"]
     [im-controls/organism-dropdown
      {:selected-value (if-let [sn (get-in @settings [:organism :shortName])]
                         sn
                         "All Organisms")
       :on-change (fn [organism]
                    (dispatch [:regions/set-selected-organism organism]))}]]))

; Input box for regions
(defn region-input-box
  "UI component allowing user to type in the regions they wish to search for"
  []
  (reagent/create-class
   (let [to-search (subscribe [:regions/to-search])
         results (subscribe [:regions/results])]
     {:reagent-render
      (fn []
        [:textarea.form-control
         {:rows (if @results 3 6)
          :placeholder (str "Type chromosome coords here, or click [Show me an example] above.")
          :value @to-search
          :on-change (fn [e]
                       (dispatch [:regions/set-to-search (oget e "target" "value")]))}])
      :component-did-mount (fn [this] (.focus (dom/dom-node this)))})))

(defn clear-textbox []
  "Interactive UI component to clear any entered text present in the region input textarea."
  (let [to-search (subscribe [:regions/to-search])]
    [css-transition-group
     {:transition-name "fade"
      :transition-enter-timeout 2000
      :transition-leave-timeout 2000
      :component "div"}
     (if @to-search
       [:div.clear-textbox
        ;;this is a fancy x-like character, aka &#10006; - NOT just x
        {:on-click #(dispatch [:regions/set-to-search nil])
         :title "Clear this textbox"} "✖"])]))

(defn region-input []
  [:div.region-input
   [:label "Regions to search "
    [tooltip {:title region-help-content-popover}
     [:svg.icon.icon-question
      [:use {:xlinkHref "#icon-question"}]]]]
   [:div.region-text
    [clear-textbox]
    [region-input-box]]])

(defn checkboxes
  "UI component ot allow user to select which types of overlapping features to find"
  [to-search settings]

  (let [all-selected? (subscribe [:regions/sequence-feature-type-all-selected?])
        results (subscribe [:regions/results])]
    [:div.checkboxes
     [:label
      [:svg.icon
       {:title (if @all-selected? "Deselect all" "Select all")
        :on-click (if @all-selected?
                    #(dispatch [:regions/deselect-all-feature-types])
                    #(dispatch [:regions/select-all-feature-types]))}
       (if @all-selected?
         [:svg.icon.icon-checkbox-checked [:use {:xlinkHref "#icon-checkbox-checked"}]]
         [:svg.icon.icon-checkbox-unchecked [:use {:xlinkHref "#icon-checkbox-unchecked"}]])] "Features to include"]
     ;; having the container around the tree is important because the tree is recursive
     ;; and we know for sure that the container is the final parent! :)
     [:div.feature-tree-container
      {:class (if @results "shrinkified")} [feature-types-tree]]]))

(defn input-section
  "Entire UI input section / top half of the region search"
  []
  (let [settings (subscribe [:regions/settings])
        to-search (subscribe [:regions/to-search])]
    [:div.row.input-section
     ; Parameters section
     [:div.row.organism-and-regions
      [region-input]
      [organism-selection]
      [:div.row
       [:div.col-md-6
        [:button.btn.btn-default.btn-raised.example-button
         {:on-click #(dispatch [:regions/set-to-search (ex)])}
         "Show Example"]]
       [:div.col-md-6
        [:button.btn.btn-primary.btn-raised.fattysubmitbutton
         {:disabled (or
                     (= "" @to-search)
                     (= nil @to-search)
                     (empty? (filter (fn [[name enabled?]] enabled?) (:feature-types @settings))))
          :on-click (fn [e] (dispatch [:regions/run-query])
                      (ocall (oget e "target") "blur"))
          :title "Enter something into the 'Regions to search' box or click on [Show me an example], then click here! :)"}
         "Search"]]]]
       ; Results section
     [checkboxes to-search settings]]))

(defn main []
  (reagent/create-class
   {:component-did-mount #(dispatch [:regions/select-all-feature-types])
    :reagent-render
    (fn []
      [:div.container.regionsearch
       [:div.headerwithguidance
        [:h1 "Region Search"]]
       [input-section]
       [results-section]])}))
