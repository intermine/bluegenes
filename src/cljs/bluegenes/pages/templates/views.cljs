(ns bluegenes.pages.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [clojure.string :refer [split join blank?]]
            [json-html.core :as json-html]
            [bluegenes.components.lighttable :as lighttable]
            [imcljs.path :as im-path]
            [bluegenes.components.ui.constraint :refer [constraint]]
            [bluegenes.components.ui.results_preview :refer [preview-table]]
            [oops.core :refer [oget ocall]]
            [clojure.string :as s]))


(defn categories []
  (let [categories (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills.template-categories
             [:li {:on-click #(dispatch [:template-chooser/set-category-filter nil])
                   :class (if (nil? @selected-category) "active")}
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
        results-preview (subscribe [:template-chooser/results-preview])]
    [:div.col-xs-8.preview
     [:h4 "Results Preview"]
     [preview-table
      :loading? @fetching-preview?
      :query-results @results-preview]
     [:button.btn.btn-primary.btn-raised.view-results
      {:type "button"
       :disabled (< (:iTotalRecords @results-preview) 1)
       :on-click (fn [] (dispatch [:templates/send-off-query]))}
      (if @fetching-preview?
        "Loading"
        (results-count-text results-preview))]]))

(defn toggle []
  (fn [{:keys [status on-change]}]
    [:div.switch-container
     [:span.switch-label "Optional"]
     [:span.switch
      [:input {:type "checkbox" :checked (case status "ON" true "OFF" false false)}]
      [:span.slider.round {:on-click on-change}]]
     [:span.switch-status status]]))

(defn select-template-settings
  "UI component to allow users to select template details, e.g. select a list to be in, lookup value greater than, less than, etc."
  [selected-template]
  (let [service (subscribe [:selected-template-service])
        row-count (subscribe [:template-chooser/count])
        lists (subscribe [:lists])]
    [:div.col-xs-4.border-right
     (into [:div.form]
           ; Only show editable constraints, but don't filter because we want the index!
           (->> (keep-indexed (fn [idx con] (if (:editable con) [idx con])) (:where @selected-template))
                (map (fn [[idx {:keys [switched switchable] :as con}]]
                       [:div.template-constraint-container
                        [constraint
                         :model (:model @service)
                         :typeahead? true
                         :path (:path con)
                         :value (or (:value con) (:values con))
                         :op (:op con)
                         :label (s/join " > " (take-last 2 (s/split (im-path/friendly (:model @service) (:path con)) #" > ")))
                         :code (:code con)
                         :hide-code? true
                         :label? true
                         :disabled (= switched "OFF")
                         :lists (second (first @lists))
                         :on-blur (fn [new-constraint]
                                    (dispatch [:template-chooser/update-preview
                                               idx (merge (cond-> con
                                                                  (contains? new-constraint :values) (dissoc :value)
                                                                  (contains? new-constraint :value) (dissoc :values)) new-constraint)]))
                         :on-change (fn [new-constraint]
                                      (dispatch [:template-chooser/replace-constraint
                                                 idx (merge (cond-> con
                                                                    (contains? new-constraint :values) (dissoc :value)
                                                                    (contains? new-constraint :value) (dissoc :values)) new-constraint)]))]
                        (when switchable
                          [toggle {:status switched
                                   :on-change (fn [new-constraint]
                                                (dispatch [:template-chooser/replace-constraint
                                                           idx (assoc con :switched (case switched "ON" "OFF" "ON"))])
                                                (dispatch [:template-chooser/update-preview]))}])]))))]))

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
         :class (if (= (name id) (:name @selected-template)) "selected")}
        ; Replace ASCII arrows in the template's title with SVG arrows
        (into [:h4]
              (->> (s/split (:title query) #" ") ; Split the title on space
                   ; Map over each part and replace any ASCII arrows with SVG icons
                   (map (fn [part]
                          (cond
                            ; If this part of text equals right arrow of any length then return an SVG right arrow
                            (re-find #"-{1,}>" part) [:svg.icon.icon-arrow-right [:use {:xlinkHref "#icon-arrow-right"}]]
                            ; Same for left arrows
                            (re-find #"<-{1,}" part) [:svg.icon.icon-arrow-left [:use {:xlinkHref "#icon-arrow-left"}]]
                            ; Otherwise just return the section of text within a span
                            :else [:span (str part " ")])))))
        [:div.description
         {:dangerouslySetInnerHTML {:__html (:description query)}}]
        (if (= (name id) (:name @selected-template))
          [:div.body
           [select-template-settings selected-template]
           [preview-results]])
        [:button.view "View >>"]
        [tags (:tags query)]
        ]])))

(defn templates
  "Outputs all the templates that match the user's chosen filters."
  []
  (fn [templates]
    (if (seq templates)
      ;;return the list of templates if there are some
      (into [:div.template-list] (map (fn [t] [template t]) templates))
      ;;if there are no templates, perhaps because of filters or perhaps not...
      [:div.no-results
       [:svg.icon.icon-wondering [:use {:xlinkHref "#icon-wondering"}]]
       " No templates available"
       (let [category-filter (subscribe [:selected-template-category])
             text-filter (subscribe [:template-chooser/text-filter])
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
         )])))

(defn template-filter []
  (let [text-filter (subscribe [:template-chooser/text-filter])]
    (fn []
      [:input.form-control.input-lg
       {:type "text"
        :value @text-filter
        :placeholder "Filter text..."
        :on-change (fn [e]
                     (dispatch [:template-chooser/set-text-filter (.. e -target -value)]))}])))

(defn filters []
  (let [me (reagent/atom nil)]
    (reagent/create-class
      {:component-did-mount (fn []
                              (let [nav-height (-> "#bluegenes-main-nav" js/$ (ocall :outerHeight true))]
                                (some-> @me (ocall :affix (clj->js {:offset {:top nav-height}})))))
       :reagent-render (fn [categories template-filter filter-state]
                         [:div.template-filters.container-fluid
                          {:ref (fn [e] (some->> e js/$ (reset! me)))}
                          [:div.template-filter
                           [:label.control-label "Filter by category"]
                           [categories]]
                          [:div.template-filter
                           [:label.control-label "Filter by description"]
                           [template-filter filter-state]]])})))


(defn main []
  (let [im-templates (subscribe [:templates-by-category])
        filter-state (reagent/atom nil)
        me (reagent/atom nil)
        filters-are-fixed? (reagent/atom nil)
        filter-height (reagent/atom nil)
        on-resize (fn []
                    ; Store the height of the child .template-filters element
                    (reset! filter-height
                            (-> @me
                                (ocall :find ".template-filters")
                                (ocall :outerHeight true))))
        on-scroll (fn []
                    ; Store whether the child .template-filers element is affixed or not
                    (reset! filters-are-fixed?
                            (-> @me
                                (ocall :find ".template-filters")
                                (ocall :hasClass "affix"))))]
    (reagent/create-class
      {:component-did-mount (fn []
                              ; Call the resize function when the component mounts
                              (on-resize)
                              ; On resize update the known value of the child filters element
                              (-> js/window js/$ (ocall :on "resize" on-resize))
                              ; On scroll update the known value of the child filters element affixed status
                              (-> js/window js/$ (ocall :on "scroll" on-scroll)))
       :component-will-unmount (fn []
                                 ; Remove the events when the component unmounts
                                 (-> js/window js/$ (ocall :off "resize" on-resize))
                                 (-> js/window js/$ (ocall :off "scroll" on-scroll)))
       :reagent-render (fn []
                         [:div.template-component-container
                          ; Store a reference to this element so we can find the child filters container
                          [:div {:ref (fn [e] (some->> e js/$ (reset! me)))}
                           [filters categories template-filter filter-state]]
                          [:div.container.template-container
                           ; Dynamically push this container down when the filters element is fixed
                           {:style {:padding-top (if @filters-are-fixed? (str @filter-height "px") 0)}}
                           [:div.row
                            [:div.col-xs-12.templates
                             [:div.template-list
                              [templates @im-templates]]]]]])})))
