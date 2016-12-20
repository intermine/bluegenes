(ns redgenes.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [clojure.string :refer [split join blank?]]
            [json-html.core :as json-html]
            [redgenes.components.imcontrols.views :refer [op-dropdown list-dropdown]]
            [redgenes.components.inputgroup :as input]
            [redgenes.components.lighttable :as lighttable]
            [imcljs.path :as im-path]
            [oops.core :refer [oget]]))


(defn categories []
  (let [categories        (subscribe [:template-chooser-categories])
        selected-category (subscribe [:selected-template-category])]
    (fn []
      (into [:ul.nav.nav-pills
             [:li {:on-click #(dispatch [:template-chooser/set-category-filter nil])
                   :class    (if (nil? @selected-category) "active")}
              [:a "All"]]]
            (map (fn [category]
                   [:li {:on-click #(dispatch [:template-chooser/set-category-filter category])
                         :class    (if (= category @selected-category) "active")}
                    [:a category]])
                 @categories)))))


(defn applies-to? [type op] (some? (some #{type} (:applies-to op))))

(defn one-of? [col value] (some? (some #{value} col)))

(def not-one-of? (complement one-of?))

(defn constraint-vertical []
  (let [state (reagent/atom {})]
    (reagent/create-class
      {:component-did-mount   (fn [this]
                                (let [[_ _ constraint] (reagent/argv this)]
                                  ; Populate the internal state atom with the constraint values.
                                  (reset! state constraint)))
       :component-will-update (fn [this new-argv]
                                ; Check the old value and the new value of the constraint.
                                ; If we went from a list constraint to a non-list constraint (or vice versa)
                                ; then reset the value of the constraint. Otherwise the name of the list
                                ; will be used as the search value in the new constraint. No bueno.
                                (let [[_ _ old-constraint] (reagent/argv this)
                                      [_ _ new-constraint] new-argv]
                                  (if (or
                                        (and
                                          (one-of? ["IN" "NOT IN"] (:op old-constraint))
                                          (not-one-of? ["IN" "NOT IN"] (:op new-constraint)))
                                        (and
                                          (one-of? ["IN" "NOT IN"] (:op new-constraint))
                                          (not-one-of? ["IN" "NOT IN"] (:op old-constraint))))
                                    (reset! state (assoc new-constraint :value nil))
                                    (reset! state new-constraint))))
       :reagent-render        (fn [idx _ friendly-name class field-type]
                                (let [change-op  (fn [e] (dispatch [:template-chooser/replace-constraint
                                                                    idx (assoc @state :op e)]))
                                      change-val (fn [e] (dispatch [:template-chooser/replace-constraint
                                                                    idx (assoc @state :value e)]))]
                                  [:div.container-fluid
                                   [:h4 friendly-name]
                                   [:div.row
                                    [:div.col-sm-4
                                     [op-dropdown @state
                                      {:type      field-type
                                       :is-class? class
                                       :on-change change-op}]]
                                    [:div.col-sm-8
                                     (if (some (partial = (:op @state)) ["IN" "NOT IN"])
                                       [list-dropdown {:value           (:value @state)
                                                       :restricted-type :Gene
                                                       :on-change       (fn [im-list] (change-val (:name im-list)))}]
                                       [:input.form-control
                                        {:type        "text"
                                         :on-change   (fn [e] (change-val (oget e :target :value)))
                                         :placeholder "Search for..."
                                         :value       (:value @state)}])]]]))})))


(defn template []
  (let [selected-template (subscribe [:selected-template])
        service           (subscribe [:selected-template-service])
        row-count (subscribe [:template-chooser/count])]
    (fn [[id query]]
      [:div.grid-1
       [:div.col.ani.template
        {:on-click (fn []
                     (if (not= (name id) (:name @selected-template))
                       (dispatch [:template-chooser/choose-template id])))
         :class    (if (= (name id) (:name @selected-template)) "selected")}
        [:div.title [:h4 (:title query)]]
        [:div.description (:description query)]
        (if (= (name id) (:name @selected-template))
          [:div.body
           [:div.col-xs-6.border-right
            (into [:form.form]
                  (map (fn [[idx con]]
                         (let [field-type (:type (last (im-path/walk (:model @service) (:path con))))
                               class      (if (im-path/class? (:model @service) (:path con))
                                            (im-path/class (:model @service) (:path con)))]
                           [constraint-vertical
                            idx con
                            (im-path/friendly (:model @service) (:path con))
                            class
                            field-type]))
                       (keep-indexed (fn [idx con]
                                       (if (:editable con)
                                         [idx con])) (:where @selected-template))))]

           [:div.col-xs-6
            {:style {:overflow-x "hidden"}}
            [:span "Results Preview"]
            [lighttable/main {:query      @selected-template
                              :service    @service
                              :no-repeats true}]
            [:button.btn.btn-primary.btn-raised
             {:type     "button"
              :on-click (fn [] (dispatch [:templates/send-off-query]))}
             (str "View All Results " "(" (js/parseInt @row-count) ")")]]])]])))

(defn templates []
  (fn [templates]
    (if (seq templates)
      ;;return the list of templates if there are some
      (into [:div] (map (fn [t] [template t]) templates))
      ;;if there are no templates, perhaps because of filters or perhaps not...
      [:div.no-results
       [:svg.icon.icon-sad [:use {:xlinkHref "#icon-sad"}]]
       " No templates available. "
       (let [category-filter (subscribe [:selected-template-category])
             text-filter     (subscribe [:template-chooser/text-filter])
             filters-active? (or (some? @category-filter) (not (blank? @text-filter)))]
         (cond filters-active?
               [:span "Try "
                [:a {:on-click
                     (fn []
                       (dispatch [:template-chooser/set-text-filter ""])
                       (dispatch [:template-chooser/set-category-filter nil])
                       )
                     } "removing the filters"]
                " to view more results. "])
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

(defn add-commas [num]
  (clojure.string/replace
    (js/String. num)
    (re-pattern "(\\d)(?=(\\d{3})+$)") "$1,"))




(defn filters [categories template-filter filter-state]
  [:div.template-filters.container-fluid
   [:div.template-filter
    [:label.control-label "Filter by category"]
    [categories]]
   [:div.template-filter
    [:label.control-label "Filter by description"]
    [template-filter filter-state]]])


(defn main []
  (let [im-templates         (subscribe [:templates-by-category])
        selected-template    (subscribe [:selected-template])
        filter-state         (reagent/atom nil)
        result-count         (subscribe [:template-chooser/count])
        counting?            (subscribe [:template-chooser/counting?])
        selected-constraints (subscribe [:template-chooser/selected-template-constraints])]
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
