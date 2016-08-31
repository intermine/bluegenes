(ns redgenes.components.listanalysis.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [redgenes.db :as db]
            [imcljs.search :as search]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path   path-constraint
                   :op     "ONE OF"
                   :values [identifier]}))

(defn ncbi-link [identifier]
  (str "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids="
       identifier))

(reg-event-db
  :listanalysis/handle-results
  (fn [db [_ widget-name results]]
    (let [with-matches-query
          (update-in results [:results]
                     (fn [data]
                       (map (fn [r]
                              (assoc r :matches-query
                                       (merge (build-matches-query
                                                (:pathQuery results)
                                                (:pathConstraint results)
                                                (:identifier r))
                                              {:title (str
                                                        (:title results)
                                                        " - "
                                                        (:description r))})))
                            data)))]
      (assoc-in db [:list-analysis :results (keyword widget-name)] with-matches-query)
      )))

(reg-fx
  :listanalysis/get-enrichment
  (fn [[widget-name results]]
    (go (dispatch [:listanalysis/handle-results widget-name (<! results)]))))

(reg-event-fx
  :listanalysis/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan
          (search/enrichment
            {:root @(subscribe [:mine-url])}
            params)]
      {:db                          db
       :listanalysis/get-enrichment [(:widget params) enrichment-chan]})))

(reg-fx
  :dispatch-many
  (fn [events]
    (doall (map dispatch events))))

(reg-event-fx
  :listanalysis/run-all
  (fn [{db :db} [_ target]]
    (let [selection (cond
                      (= :query (:type target)) {:ids (:values (first (:where (:value target))))}
                      (= :list (:type target)) {:list (:value target)})]
      {:db            (assoc-in db [:list-analysis :target] target)
       :dispatch-many [[:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "pathway_enrichment"
                                                            :correction "Holm-Bonferroni"})]
                       [:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "go_enrichment_for_gene"
                                                            :correction "Holm-Bonferroni"})]
                       [:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "prot_dom_enrichment_for_gene"
                                                            :correction "Holm-Bonferroni"})]
                       [:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "publication_enrichment"
                                                            :correction "Holm-Bonferroni"})]
                       [:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "bdgp_enrichment"
                                                            :correction "Holm-Bonferroni"})]
                       [:listanalysis/run (merge selection {:maxp       0.05
                                                            :widget     "miranda_enrichment"
                                                            :correction "Holm-Bonferroni"})]]})))
