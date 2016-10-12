(ns redgenes.sections.results.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as filters]
            [imcljs.search :as search]
            [clojure.spec :as s]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [redgenes.sections.saveddata.events :as sd]))


(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path   path-constraint
                   :op     "ONE OF"
                   :values [identifier]}))

(reg-event-fx
  :results/set-text-filter
  (fn [{db :db} [_ value]]
    {:db (assoc-in db [:results :text-filter] value)}))

(reg-event-fx
  :results/set-query
  (fn [{db :db} [_ query]]
    (let [model (get-in db [:assets :model])]
      {:db       (update-in db [:results] assoc
                            :query query
                            :query-parts (filters/get-parts model query)
                            :enrichment-results nil)
       :dispatch [:results/enrich]})))

(reg-event-fx
  :results/enrich
  (fn [{db :db}]
    (let [query-parts (get-in db [:results :query-parts])
          can-enrich? (contains? query-parts :Gene)]
      (if can-enrich?
        (let [enrich-query (-> query-parts :Gene first :query)]
          {:db                   db
           :fetch-ids-from-query enrich-query})
        {:db db}))))

(reg-fx
  :fetch-ids-from-query
  (fn [query]
    (go (let [{results :results} (<! (search/raw-query-rows
                                       {:root "http://beta.flymine.org/query"}
                                       query))]
          (dispatch [:success-fetch-ids (flatten results)])))))

(reg-event-fx
  :success-fetch-ids
  (fn [{db :db} [_ results]]
    {:db       (assoc-in db [:results :ids-to-enrich] results)
     :dispatch [:results/run-all-enrichment-queries]}))

(reg-event-fx
  :results/run-all-enrichment-queries
  (fn [{db :db}]
    (let [selection {:ids (get-in db [:results :ids-to-enrich])}]
      {:db         db
       :dispatch-n [[:results/run (merge selection {:maxp       0.05
                                                    :widget     "pathway_enrichment"
                                                    :correction "Holm-Bonferroni"})]
                    [:results/run (merge selection {:maxp       0.05
                                                    :widget     "go_enrichment_for_gene"
                                                    :correction "Holm-Bonferroni"})]
                    [:results/run (merge selection {:maxp       0.05
                                                    :widget     "prot_dom_enrichment_for_gene"
                                                    :correction "Holm-Bonferroni"})]
                    [:results/run (merge selection {:maxp       0.05
                                                    :widget     "publication_enrichment"
                                                    :correction "Holm-Bonferroni"})]
                    [:results/run (merge selection {:maxp       0.05
                                                    :widget     "bdgp_enrichment"
                                                    :correction "Holm-Bonferroni"})]
                    [:results/run (merge selection {:maxp       0.05
                                                    :widget     "miranda_enrichment"
                                                    :correction "Holm-Bonferroni"})]]})))


(reg-event-fx
  :results/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan
          (search/enrichment
            {:root @(subscribe [:mine-url])}
            params)]
      {:db                     db
       :results/get-enrichment [(:widget params) enrichment-chan]})))

(reg-fx
  :results/get-enrichment
  (fn [[widget-name results]]
    (go (dispatch [:results/handle-results widget-name (<! results)]))))


(reg-event-db
  :results/handle-results
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
      (assoc-in db [:results :enrichment-results (keyword widget-name)] with-matches-query))))

