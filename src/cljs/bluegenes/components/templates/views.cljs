(ns bluegenes.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [clojure.string :refer [split join blank?]]
            [json-html.core :as json-html]
            [bluegenes.components.imcontrols.views :refer [op-dropdown list-dropdown]]
            [bluegenes.components.inputgroup :as input]
            [bluegenes.components.lighttable :as lighttable]
            [imcljs.path :as im-path]
            [bluegenes.components.ui.constraint :refer [constraint]]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [oops.core :refer [oget]]))


(defn categories []
  (let [categories        (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills.template-categories
             [:li {:on-click #(dispatch [:template-chooser/set-category-filter nil])
                   :class    (if (nil? @selected-category) "active")}
              [:a.type-all "All"]]]
            (map (fn [category]
                   [:li {:on-click #(dispatch [:template-chooser/set-category-filter category])
                         :class
             (if (= category @selected-category) " active")}
                    [:a {:class (str
                               "type-" category)} category]])
                 @categories)))))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn results-count-text [results-preview]
  (if (< (:iTotalRecords @results-preview) 1)
    "No Results"
    (str "View "
         (js/parseInt (:iTotalRecords @results-preview))
         (if (> (:iTotalRecords @results-preview) 1) " rows" " row"))))

(defn preview-results
  "Preview results of template as configured by the user or default config"
  [results-preview fetching-preview]
  (let [fetching-preview? (subscribe [:template-chooser/fetching-preview?])
        results-preview   (subscribe [:template-chooser/results-preview])]
    [:div.col-xs-8.preview
     [:h4 "Results Preview"]
     [preview-table
      :loading? @fetching-preview?
      :query-results @results-preview]
     [:button.btn.btn-primary.btn-raised.view-results
      {:type     "button"
       :disabled (< (:iTotalRecords @results-preview) 1)
       :on-click (fn [] (dispatch [:templates/send-off-query]))}
      (if @fetching-preview?
        "Loading"
        (results-count-text results-preview))]]))

(defn select-template-settings
  "UI component to allow users to select template details, e.g. select a list to be in, lookup value grater than, less than, etc."
  [selected-template]
  (let [service   (subscribe [:selected-template-service])
        row-count (subscribe [:template-chooser/count])
        lists     (subscribe [:lists])]
    [:div.col-xs-4.border-right
     (into [:form.form]
           ; Only show editable constraints, but don't filter because we want the index!
           (->> (keep-indexed (fn [idx con] (if (:editable con) [idx con])) (:where @selected-template))
                (map (fn [[idx con]]
                       [constraint
                        :model (:model @service)
                        :typeahead? false
                        :path (:path con)
                        :value (:value con)
                        :op (:op con)
                        :label? true
                        :lists (second (first @lists))
                        :on-change (fn [new-constraint]
                                     (dispatch [:template-chooser/replace-constraint
                                                idx (merge con new-constraint)]))]))))]))

(defn tags
  "UI element to visually output all aspect tags into each template card for easy scanning / identification of tags.
  ** Expects: format im:aspect:thetag.
  ** Will output 'thetag'.
  ** Won't output any other tag types or formats"
  [tagvec]
  (into
   [:div.template-tags "Template categories: "]
    (if (> (count tagvec) 0)
        (map (fn [tag]
               (let [tag-parts (clojure.string/split tag #":")
                     tag-name (peek tag-parts)
                     is-aspect (and (= 3 (count tag-parts)) (= "aspect" (nth tag-parts 1)))]
               (if is-aspect
                 [:span.tag-type {:class (str "type-" tag-name)} tag-name]
                 nil)
                 )) tagvec)
      " none"
      )
   ))

(defn template
  "UI element for a single template." []
  (let [selected-template (subscribe [:selected-template])]
    (fn [[id query]]
      [:div.grid-1
       [:div.col.ani.template
        {:on-click (fn []
                     (if (not= (name id) (:name @selected-template))
                       (dispatch [:template-chooser/choose-template id])))
         :class    (if (= (name id) (:name @selected-template)) "selected")}
        [:div.title [:h4 (:title query)]]
        [:div.description
         {:dangerouslySetInnerHTML {:__html (:description query)}}]
        (if (= (name id) (:name @selected-template))
          [:div.body
           [select-template-settings selected-template]
           [preview-results]])
        [tags (:tags query)]
        ]])))

(defn templates
  "Outputs all the templates that match the user's chosen filters."
  []
  (fn [templates]
    (if (seq templates)
      ;;return the list of templates if there are some
      (into [:div] (map (fn [t] [template t]) templates))
      ;;if there are no templates, perhaps because of filters or perhaps not...
      [:div.no-results
       [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
       " No templates available"
       (let [category-filter (subscribe [:selected-template-category])
             text-filter     (subscribe [:template-chooser/text-filter])
             filters-active? (or (some? @category-filter) (not (blank? @text-filter)))]
         (cond filters-active?

               [:span
                [:span
                 (cond @category-filter
                       (str " in the '" @category-filter "' category"))
                 (cond @text-filter
                       (str " containing the text '" @text-filter "'"))]
                [:span ". Try "
                 [:a {:on-click
                      (fn []
                        (dispatch [:template-chooser/set-text-filter ""])
                        (dispatch [:template-chooser/set-category-filter nil]))
                      } "removing the filters"]
                 " to view more results. "]])
         )
       ])))

(defn template-filter []
  (let [text-filter (subscribe [:template-chooser/text-filter])]
    (fn []
      [:input.form-control.input-lg
       {:type        "text"
        :value       @text-filter
        :placeholder "Filter text..."
        :on-change   (fn [e]
                       (dispatch [:template-chooser/set-text-filter (.. e -target -value)]))}])))


(defn filters [categories template-filter filter-state]
  [:div.template-filters.container-fluid
   [:div.template-filter
    [:label.control-label "Filter by category"]
    [categories]]
   [:div.template-filter
    [:label.control-label "Filter by description"]
    [template-filter filter-state]]])


(defn main []
  (let [im-templates (subscribe [:templates-by-category])
        filter-state (reagent/atom nil)]
    (fn []
      [:div.container-fluid
       ;(json-html/edn->hiccup @selected-template)
       [:div.row
        [:div.col-xs-12.templates
         [filters categories template-filter filter-state]
         [:div.template-list
          ;;the bad placeholder exists to displace content, but is invisible. It's a duplicate of the filters header
          [:div.bad-placeholder [filters categories template-filter filter-state]]
          [templates @im-templates]]]
        ]])))
