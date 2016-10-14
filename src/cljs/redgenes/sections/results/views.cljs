(ns redgenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.results.events]
            [redgenes.sections.results.subs]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]))


(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))


(def enrichment-config {:pathway_enrichment           {:title   "Pathways"
                                                       :returns [{:header "Pathway" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :go_enrichment_for_gene       {:title   "Gene Ontology"
                                                       :returns [{:header "GO Term" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :prot_dom_enrichment_for_gene {:title   "Protein Domains"
                                                       :returns [{:header "Protein Domain" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :publication_enrichment       {:title   "Publications"
                                                       :returns [{:header "Protein Domain" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :bdgp_enrichment              {:title   "BDGPs"
                                                       :returns [{:header "Terms" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :miranda_enrichment           {:title   "MiRNA"
                                                       :returns [{:header "X" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}})

(def sidebar-hover (reagent/atom false))

(defn popover-table []
  (let [values (subscribe [:results/summary-values])
        result (first (:results @values))
        column-headers (:columnHeaders @values)]
    (fn [matches p-value]
     [:div
       [:table
        (into [:div]
              (map-indexed (fn [idx header]
                             [:div.popover-contents
                              [:div.title (last (clojure.string/split header " > "))]
                              [:div.value (get result idx)]]) column-headers))]])))



(defn enrichment-result-row []
  (fn [{:keys [description matches identifier p-value matches-query] :as row}
       {:keys [pathConstraint] :as details}]
    [:li.enrichment-item
     {:on-mouse-enter (fn [] (dispatch [:results/get-item-details identifier pathConstraint]))
      :on-click       (fn []

                        (dispatch [:results/add-to-history row details]))}
     [popover [:span {:data-content   [popover-table matches p-value]
                      :data-placement "left"
                      :data-trigger   "hover"}
               description]]]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description details]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(defn enrichment-results-preview []
  (let [text-filter (subscribe [:results/text-filter])]
    (fn [[widget-name {:keys [results] :as details}]]
      ;(.log js/console "RESULTS" results)
      [:div
       [:h4
        {:class (if (empty? results) "greyout")}
        (str (get-in enrichment-config [widget-name :title])
             " (" (count results) ")")]
       (into [:ul.enrichment-list]
             (map (fn [row] [enrichment-result-row row details])
                  (take 5 (filter
                            (fn [{:keys [description]}]
                              (has-text? @text-filter description))
                            results))))])))




(defn enrichment-results []
  (let [all-enrichment-results (subscribe [:results/enrichment-results])]
    (fn []
      (if (nil? (vals @all-enrichment-results))
        [:div [:h4 "No Results"]]
        [:div.demo
         [css-transition-group
          {:transition-name          "fade"
           :transition-enter-timeout 500
           :transition-leave-timeout 500}
          (map (fn [enrichment-response]
                 ^{:key (first enrichment-response)} [enrichment-results-preview enrichment-response])
               @all-enrichment-results)]]))))

(defn text-filter []
  (let [value (subscribe [:results/text-filter])]
    [:input.form-control.input-lg
     {:type        "text"
      :value       @value
      :on-change   (fn [e]
                     (let [value (.. e -target -value)]
                       (if (or (= value "") (= value nil))
                         (dispatch [:results/set-text-filter nil])
                         (dispatch [:results/set-text-filter value]))))
      :placeholder "Filter..."}]))

(defn side-bar []
  (let [query-parts (subscribe [:results/query-parts])
        value       (subscribe [:results/text-filter])]
    (fn []
      [:div.sidebar
       {:on-mouse-enter (fn [] (reset! sidebar-hover true))
        :on-mouse-leave (fn [] (reset! sidebar-hover false))}
       [:div
        [:h3.inline "Enrichment Statistics"]
        [tooltip [:i.fa.fa-question-circle
                  {:title          "The p-value is the probability that result occurs by chance, thus a lower p-value indicates greater enrichment."
                   :data-trigger   "hover"
                   :data-placement "bottom"}]]]
       [:div.expandable
        {:class (if (or @sidebar-hover @value) "present" "gone")}
        [text-filter]]
       [enrichment-results]])))

(defn adjust-str-to-length [length string]
  (if (< length (count string)) (str (clojure.string/join (take (- length 3) string)) "...") string))

(defn breadcrumb []
  (let [history       (subscribe [:results/history])
        history-index (subscribe [:results/history-index])]
    (fn []
      [:div.breadcrumb-container
       [:i.fa.fa-clock-o]
       (into [:ul.breadcrumb.inline]
             (map-indexed
               (fn [idx {title :title}]
                 (let [adjsuted-title (adjust-str-to-length 20 title)]
                   [:li
                    {:class (if (= @history-index idx) "active")}
                    [tooltip
                     [:a
                      {:data-placement "bottom"
                       :title          title
                       :on-click       (fn [x] (dispatch [:results/load-from-history idx]))} adjsuted-title]]])) @history))])))




(defn main []
  (let [query (subscribe [:results/query])]
    (fn []
      [:div.container
       [breadcrumb]
       [:div.row
        [:div.col-md-9.col-sm-12
         [:div.panel.panel-default
          [:div.panel-body.autoscroll
           (if @query [table/main @query true])]]]
        [:div.col-md-3.col-sm-12
         [side-bar]
         ]]])))






;[:button.btn.btn-primary.btn-raised
; {:on-click (fn []
;              (dispatch
;                [:save-data {:sd/type    :query
;                             :sd/service :flymine
;                             :sd/label   (last (split (:title @query) "-->"))
;                             :sd/value   (assoc @query :title (last (split (:title @query) "-->")))}]))} "Save"]