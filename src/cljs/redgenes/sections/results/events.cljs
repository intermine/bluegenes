(ns redgenes.sections.results.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as filters]
            [imcljs.search :as search]
            [clojure.spec :as s]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [redgenes.interceptors :refer [clear-tooltips]]
            [dommy.core :refer-macros [sel sel1]]
            [redgenes.sections.saveddata.events]))




(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path   path-constraint
                   :op     "ONE OF"
                   :values [identifier]}))

; Could be useful later?
(reg-event-db
  :worker
  (fn [db]
    (let [worker (-> (js/Worker. "/workers/filtertext.js")
                     (aset "onmessage" (fn [r] (println "result" (.. r -data)))))]
      (.postMessage worker (clj->js ["e" "title" [{:title "the little book of calm"}
                                                  {:title "the cat in the hat"}
                                                  {:title "sailing for dummies"}]])))
    db))

(reg-event-db
  :save-summary-fields
  (fn [db [_ response]]
    (assoc-in db [:results :summary-values] response)))

(reg-fx
  :get-summary-values
  (fn [c]
    (go (dispatch [:save-summary-fields (<! c)]))))

(reg-event-fx
  :results/get-item-details
  (fn [{db :db} [_ identifier path-constraint]]
    (let [model          (get-in db [:assets :model])
          class          (keyword (filters/end-class model path-constraint))
          summary-fields (get-in db [:assets :summary-fields class])
          summary-chan   (search/raw-query-rows
                           {:root @(subscribe [:mine-url])}
                           {:from   class
                            :select summary-fields
                            :where  [{:path  (last (clojure.string/split path-constraint "."))
                                      :op    "="
                                      :value identifier}]})]
      {:db                 (assoc-in db [:results :summary-chan] summary-chan)
       :get-summary-values summary-chan})))

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
                            :history [query]
                            :history-index 0
                            :query-parts (filters/get-parts model query)
                            :enrichment-results nil)
       :dispatch ^:flush-dom [:results/enrich]})))


(reg-event-fx
  :results/add-to-history
  [(clear-tooltips)]
  (fn [{db :db} [_ {identifier :identifier} details]]
    (let [model    (get-in db [:assets :model])
          previous (get-in db [:results :query])
          query    (merge (build-matches-query
                            (:pathQuery details)
                            (:pathConstraint details)
                            identifier)
                          {:title (str
                                    (:title details)
                                    " - "
                                    (:description details))})]
      {:db       (-> db
                     (update-in [:results :history] conj query)
                     (update-in [:results] assoc
                                :query query
                                :history-index (inc (get-in db [:results :history-index]))
                                :query-parts (filters/get-parts model query)
                                :enrichment-results nil))
       :dispatch [:results/enrich]})))

(reg-event-fx
  :results/load-from-history
  (fn [{db :db} [_ index]]
    (let [model (get-in db [:assets :model])
          query (get-in db [:results :history index])]
      {:db       (-> db
                     (update-in [:results] assoc
                                :query query
                                :history-index index
                                :query-parts (filters/get-parts model query)
                                :enrichment-results nil))
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

(reg-event-fx
  :results/update-enrichment-setting
  (fn [{db :db} [_ setting value]]
    {:db       (assoc-in db [:results :enrichment-settings setting] value)
     :dispatch [:results/run-all-enrichment-queries]}))

(reg-fx
  :fetch-ids-from-query
  (fn [query]
    (go (let [{results :results} (<! (search/raw-query-rows
                                       {:root @(subscribe [:mine-url])}
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
    (let [selection {:ids (get-in db [:results :ids-to-enrich])}
          settings  (get-in db [:results :enrichment-settings])]
      {:db         db
       :dispatch-n [[:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "pathway_enrichment"
                                     :correction "Holm-Bonferroni"}
                                    settings)]
                    [:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "go_enrichment_for_gene"
                                     :correction "Holm-Bonferroni"}
                                    settings)]
                    [:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "prot_dom_enrichment_for_gene"
                                     :correction "Holm-Bonferroni"}
                                    settings)]
                    [:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "publication_enrichment"
                                     :correction "Holm-Bonferroni"}
                                    settings)]
                    [:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "bdgp_enrichment"
                                     :correction "Holm-Bonferroni"}
                                    settings)]
                    [:results/run (merge
                                    selection
                                    {:maxp       0.05
                                     :widget     "miranda_enrichment"
                                     :correction "Holm-Bonferroni"}
                                    settings)]]})))



(reg-event-fx
  :results/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan (search/enrichment {:root @(subscribe [:mine-url])} params)]
      {:db                     db
       :results/get-enrichment [(:widget params) enrichment-chan]})))

(reg-fx
  :results/get-enrichment
  (fn [[widget-name results]]
    (go (dispatch [:results/handle-results widget-name (<! results)]))))


(reg-event-db
  :results/handle-results
  (fn [db [_ widget-name results]]
    (assoc-in db [:results :enrichment-results (keyword widget-name)] results)))
