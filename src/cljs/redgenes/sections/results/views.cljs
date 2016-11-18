(ns redgenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.results.events]
            [redgenes.sections.results.subs]
            [redgenes.components.bootstrap :refer [popover tooltip]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]
            [im-tables.views.core :as tables]))


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
  (let [values         (subscribe [:results/summary-values])
        result         (first (:results @values))
        column-headers (:columnHeaders @values)]
    (fn [matches p-value]
      [:div.sidebar-popover
       [:table
        (into [:tbody]
              (map-indexed (fn [idx header]
                             [:tr.popover-contents.sidebar-popover
                              [:td.title (last (clojure.string/split header " > "))]
                              [:td.value (get result idx)]]) column-headers))]])))



(defn enrichment-result-row []
  (fn [{:keys [description matches identifier p-value matches-query] :as row}
       {:keys [pathConstraint] :as details}]
    [:li.enrichment-item
     {:on-mouse-enter
      (fn [] (dispatch [:results/get-item-details identifier pathConstraint]))
      :on-click
      (fn []
        (dispatch [:results/add-to-history row details]))}
     [:div.container-fluid
      [:div.row
       [:div.col-xs-8
        ; TODO popovers are causing reagent key ID errors
        #_[popover [:span {:data-content   [popover-table matches p-value]
                           :data-placement "left"
                           :data-trigger   "hover"}
                    [:span description]]]
        [:span {:data-content   [popover-table matches p-value]
                :data-placement "left"
                :data-trigger   "hover"}
         [:span description]]]
       [:div.col-xs-4 [:span {:style {:font-size "0.8em"}} (.toExponential p-value 6)]]]]
     ]))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description details]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(defn enrichment-results-preview []
  (let [page-state  (reagent/atom {:page 0 :show 5})
        text-filter (subscribe [:results/text-filter])]
    (fn [[widget-name {:keys [results] :as details}]]
      [:div
       [:h4 {:class (if (empty? results) "greyout")}
        (str (get-in enrichment-config [widget-name :title]))
        (if results
          [:span
           [:span (if results (str " (" (count results) ")"))]
           (if (< 0 (count results))
             [:span
              [:i.fa.fa-caret-left
               {:on-click (fn [] (swap! page-state update :page dec))}]
              [:i.fa.fa-caret-right
               {:on-click (fn [] (swap! page-state update :page inc))}]])]
          [:span [:i.fa.fa-cog.fa-spin]])]


       (into [:ul.enrichment-list]
             (map (fn [row] [enrichment-result-row row details])
                  (take (:show @page-state) (filter
                                              (fn [{:keys [description]}]
                                                (has-text? @text-filter description))
                                              (drop (* (:page @page-state) (:show @page-state)) results)))))])))


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
          [:div.container-fluid
           [:div.row
            [:div.col-xs-8 "Item"]
            [:div.col-xs-4 "p-value"]]]
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
       [:div.expandable.present
        ; disable hover effects for now
        {:class (if (or @sidebar-hover @value) "present" "gone")}


        [:div.container-fluid
         {:width "100%"}
         [:div.row
          [:div.col-xs-4
           [:label "Max p-value"]
           [:select.form-control
            {:on-change #(dispatch [:results/update-enrichment-setting :maxp (oget % "target" "value")])}
            [:option "0.05"]
            [:option "0.10"]
            [:option "1.00"]]]
          [:div.col-xs-8
           [:label "Test Correction"]
           [:select.form-control
            {:on-change #(dispatch [:results/update-enrichment-setting :correction (oget % "target" "value")])}
            [:option "Holm-Bonferroni"]
            [:option "Benjamini Hochber"]
            [:option "Bonferroni"]
            [:option "None"]]]]
         [:div.row
          [:div.col-xs-12
           [text-filter]]]]]
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
               (fn [idx {{title :title} :value}]
                 (let [adjsuted-title (if (not= idx @history-index) (adjust-str-to-length 20 title) title)]
                   [:li {:class (if (= @history-index idx) "active")}
                    [tooltip
                     [:a
                      {:data-placement "bottom"
                       :title          title
                       :on-click       (fn [x] (dispatch [:results/load-from-history idx]))} adjsuted-title]]])) @history))])))




(defn main []
  (let [package-for-table (subscribe [:results/package-for-table])]
    (fn []
      [:div.container
       [breadcrumb]
       [:div.row
        [:div.col-md-9.col-sm-12
         [:div.panel.panel-default
          [:div.panel-body
           [tables/main [:results :fortable]]
           ;(if @query [table/main @package-for-table true])
           ]]]
        [:div.col-md-3.col-sm-12
         [side-bar]]]])))






;[:button.btn.btn-primary.btn-raised
; {:on-click (fn []
;              (dispatch
;                [:save-data {:sd/type    :query
;                             :sd/service :flymine
;                             :sd/label   (last (split (:title @query) "-->"))
;                             :sd/value   (assoc @query :title (last (split (:title @query) "-->")))}]))} "Save"]
