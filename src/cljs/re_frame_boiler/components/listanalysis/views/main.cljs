(ns re-frame-boiler.components.listanalysis.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [re-frame-boiler.components.listanalysis.events]
            [re-frame-boiler.components.listanalysis.subs]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(def enrichment-config {:pathway_enrichment           {:title   "Pathway Enrichment"
                                                       :returns [{:header "Pathway" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :go_enrichment_for_gene       {:title   "Gene Ontology Enrichment"
                                                       :returns [{:header "GO Term" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :prot_dom_enrichment_for_gene {:title   "Protein Domain Enrichment"
                                                       :returns [{:header "Protein Domain" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :publication_enrichment       {:title   "Publication Enrichment"
                                                       :returns [{:header "Protein Domain" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :bdgp_enrichment              {:title   "BDGP Enrichment"
                                                       :returns [{:header "Terms" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}
                        :miranda_enrichment           {:title   "MiRNA Enrichment"
                                                       :returns [{:header "X" :field :description}
                                                                 {:header "Matches" :field :matches}
                                                                 {:header "p-value" :field :p-value}]}})


(defn results []
  (let [results (subscribe [:listanalysis/results-all])]
    (fn []
      [:div (json-html/edn->hiccup @results)])))

(defn results-row []
  (fn [data]
    (into [:tr]
          (map (fn [{field :field}] [:td (field data)]) (-> enrichment-config :pathway_enrichment :returns)))))

(defn results-table []
  (fn [type results]
    (if (empty? results)
      [:div.alert.alert-warning "No Results"]
      [:table.table
      [:thead
       (into [:tr]
             (map (fn [header]
                    [:th (:header header)])
                  (-> enrichment-config type :returns)))]
      (into [:tbody]
            (map (fn [result] [results-row result])
                 results))])))

(defn controls []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-primary
      {:on-click (fn [] (dispatch [:listanalysis/run]))} "Run"]]))


(defn loading []
  [:i.fa.fa-cog.fa-spin.fa-3x.fa-fw])

(defn list-analysis [type]
  (let [results           (subscribe [:listanalysis/results type])
        enrichment-config (-> enrichment-config type)]
    (fn []
      [:div.panel.panel-default.enrichment
       [:div.panel-heading (:title enrichment-config)]
       (if-not @results
         [:div.panel-body [:div.table-container [loading]]]
         [:div.panel-body
          [:form.form.form-sm
           [:div.row
            [:div.col-sm-5.form-group.form-xs
             [:label.control-label "Test Correction"]
             [:select.form-control
              [:option "Holm-Bonferroni"]
              [:option "Benjamini Hochber"]
              [:option "Bonferroni"]
              [:option "None"]]]
            [:div.col-sm-3.form-group
             [:label.control-label "Max p-value"]
             [:select.form-control
              [:option 0.05]
              [:option 0.10]
              [:option 1.00]]]
            [:div.col-sm-4.form-group
             [:label.control-label "Background Population"]
             [:select.form-control
              [:option 1]
              [:option 2]
              [:option 3]
              [:option 4]]]]
           ]
          [:div.table-container
           [results-table type (:results @results)]]
          ;[controls]
          ])

       ])))

(defn main []
  (fn [type]
    [:div
     [list-analysis type]
     ;[results]
     ]))



#_[:div.row
   [:div.col-sm-12
    [:div.form-group
     [:label.control-label "Filter"]
     [:input.form-control {:type        "text"
                           :value       @textf
                           :placeholder "Filter text..."
                           :on-change   (fn [e] (reset! textf (.. e -target -value)))}]]]]

#_[results-table type
   (if @textf
     (filter (fn [res]
               (re-find (re-pattern (str "(?i)" @textf)) (:description res))) (:results @results))
     (:results @results))]