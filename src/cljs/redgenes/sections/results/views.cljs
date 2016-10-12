(ns redgenes.sections.results.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [redgenes.components.table :as table]
            [redgenes.sections.results.events]
            [redgenes.sections.results.subs]
            [clojure.string :refer [split]]))

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

(defn popover []
  (reagent/create-class
    {:component-did-mount
     (fn [this]
       (let [node (reagent/dom-node this)] (.popover (-> node js/$))))
     :reagent-render
     (fn [[element attributes & rest]]
       [element (-> attributes
                    (assoc :data-html true)
                    (assoc :data-container "body")
                    (update :data-content reagent/render-to-static-markup)) rest])}))

(defn tooltip []
  (reagent/create-class
    {:component-did-mount
     (fn [this]
       (let [node (reagent/dom-node this)] (.tooltip (-> node js/$))))
     :reagent-render
     (fn [[element attributes & rest]]
       [element attributes rest])}))


(defn enrichment-result-row []
  (fn [{:keys [description matches p-value]}]
    [:li.enrichment-item
     [popover
      [:span {:data-content   [:div
                               [:table
                                [:tbody
                                 [:tr
                                  [:td "Matches"]
                                  [:td matches]]
                                 [:tr
                                  [:td "p-value"]
                                  [:td p-value]]]]]
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
    (fn [widget-name results]
      [:div
       [:h4
        {:class (if (empty? results) "greyout")}
        (str (get-in enrichment-config [widget-name :title])
             " (" (count results) ")")]
       (into [:ul.enrichment-list]
             (map (fn [row] [enrichment-result-row row])
                  (take 5 (filter
                            (fn [{:keys [description]}]
                              (has-text? @text-filter description))
                            results))))])))


(defn enrichment-results []
  (let [all-enrichment-results (subscribe [:results/enrichment-results])]
    (fn []
      (into [:div] (map (fn [[widget-name details]]
                          [enrichment-results-preview widget-name (:results details)]) @all-enrichment-results)))))

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
    (reagent/create-class
      {:component-did-mount
       (fn [this])
       :reagent-render
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
          [enrichment-results]])})))

(defn main []
  (let [query (subscribe [:results/query])]
    (fn []
      [:div.container
       [:div.row
        [:div.col-md-9.col-sm-12
         [:div.panel.panel-default
          [:div.panel-body
           (if @query [table/main @query true])]]]
        [:div.col-md-3.col-sm-12
         [side-bar]]]])))

;[:button.btn.btn-primary.btn-raised
; {:on-click (fn []
;              (dispatch
;                [:save-data {:sd/type    :query
;                             :sd/service :flymine
;                             :sd/label   (last (split (:title @query) "-->"))
;                             :sd/value   (assoc @query :title (last (split (:title @query) "-->")))}]))} "Save"]