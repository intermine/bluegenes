(ns redgenes.components.templates.views
  (:require [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [clojure.string :refer [split join blank?]]
            [json-html.core :as json-html]
            [redgenes.components.imcontrols.views :refer [op-dropdown]]
            [redgenes.components.inputgroup :as input]
            [redgenes.components.lighttable :as lighttable]))

(def ops [{:op         "="
           :applies-to [:string :boolean :integer :double :float]}
          {:op         "!="
           :applies-to [:string :boolean :integer :double :float]}
          {:op         "CONTAINS"
           :applies-to [:string]}
          {:op         "<"
           :applies-to [:integer :double :float]}
          {:op         "<="
           :applies-to [:integer :double :float]}
          {:op         ">"
           :applies-to [:integer :double :float]}
          {:op         ">="
           :applies-to [:integer :double :float]}
          {:op         "LIKE"
           :applies-to [:string]}
          {:op         "NOT LIKE"
           :applies-to [:string]}
          {:op         "ONE OF"
           :applies-to []}
          {:op         "NONE OF"
           :applies-to []}
          {:op         "LOOKUP"
           :applies-to [:class]}])


(defn list-dropdown []
  (let [lists (subscribe [:lists])]
    (fn [update-fn]
      [:div.dropdown
       [:button.btn.btn-primary.dropdown-toggle {:type "button" :data-toggle "dropdown"}
        [:i.fa.fa-list.pad-right-5] "List"]
       (into [:ul.dropdown-menu.dropdown-menu-right]
             (map (fn [l]
                    [:li
                     {:on-click (fn [] (update-fn (:name l)))}
                     [:a (str (:name l))]]) @lists))])))

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

(defn constraint-vertical [idx state]
  (let [state (reagent/atom state)]
    (fn [idx constraint]

      (.log js/console "CONSTRAINT" constraint)
      [:div
       [:label [:span (join " > " (take-last 2 (split (:path constraint) ".")))]]
       [:div.input-group
        [:span.input-group-btn
         [op-dropdown constraint
          {:type      :string
           :on-change (fn [x]
                        (dispatch [:template-chooser/replace-constraint idx (assoc constraint :op x)])
                        )}]]
        [:input.form-control
         {:type        "text"
          :placeholder "Search for..."
          :value (:value constraint)}]]])))

(defn template []
  (let [selected-template (subscribe [:selected-template])
        service           (subscribe [:selected-template-service])]
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
                  (concat
                    (map (fn [[idx con]]
                           [constraint-vertical idx con])
                         (keep-indexed (fn [idx con]
                                         (if (:editable con)
                                           [idx con])) (:where query)))
                    [[:div.form-group.row
                      [:div.col-xs-offset-6
                       {:style {:text-align "right"}}
                       [:button.btn.btn-primary.btn-raised
                        {:type     "button"
                         :on-click (fn [] (dispatch [:templates/send-off-query]))}
                        "View All Results"]]]]))]

           [:div.col-xs-6
            {:style {:overflow-x "hidden"}}
            [:span "Results Preview"]
            [lighttable/main {:query      @selected-template
                              :service    @service
                              :no-repeats true}]]])]])))

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
    [template-filter filter-state]]
   ])


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
