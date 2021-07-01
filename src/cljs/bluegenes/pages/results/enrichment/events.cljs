(ns bluegenes.pages.results.enrichment.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [clojure.string :as string]
            [imcljs.fetch :as fetch]
            [imcljs.path :as path]
            [clojure.set :as set]
            [bluegenes.pages.results.events :refer [clear-widget-options]]))

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path path-constraint
                   :op "ONE OF"
                   :values (if (coll? identifier)
                             (vec identifier)
                             (vector identifier))}))

(reg-event-fx
 :enrichment/get-item-details
 (fn [{db :db} [_ identifier path-constraint]]
   (let [source (get-in db [:results :package :source])
         model (get-in db [:mines source :service :model])
         classname (keyword (path/class model path-constraint))
         summary-fields (get-in db [:assets :summary-fields source classname])
         service (get-in db [:mines source :service])
         summary-chan (fetch/rows
                       service
                       {:from (name classname)
                        :select summary-fields
                        :where [{:path (last (clojure.string/split path-constraint "."))
                                 :op "="
                                 :value identifier}]})]
     {:db (assoc-in db [:results :summary-chan] summary-chan)
      :get-summary-values [identifier summary-chan]})))

(reg-event-fx
 :enrichment/set-text-filter
 (fn [{db :db} [_ value]]
   {:db (assoc-in db [:results :text-filter] value)}))

(defn what-we-can-enrich
  "Returns list of columns in the results view that have widgets available for enrichment."
  [widgets query-parts]
  (let [possible-roots (set (keys query-parts))
        possible-enrichments (reduce (fn [x y]
                                       (conj x (keyword (first (:targets y)))))
                                     #{}
                                     (filter #(= "enrichment" (:widgetType %)) widgets))
        enrichable-roots (set/intersection possible-enrichments possible-roots)]
    (select-keys query-parts enrichable-roots)))

(reg-event-fx
 :enrichment/update-active-enrichment-column
 (fn [{db :db} [_ new-enrichment-column]]
   {:db (-> db
            (assoc-in [:results :active-enrichment-column] new-enrichment-column)
             ;;we need to remove the old results to prevent them showing up when a new
             ;;column setting has been selected. Fixes Issue #52.
            (update-in [:results] dissoc :enrichment-results)
            (assoc-in [:results :enrichment-results-loading?] true)
            (clear-widget-options))
    :dispatch [:enrichment/enrich]}))

(defn can-we-enrich-on-existing-preference?
  "Returns whether or not the previously used enrichment column is within the list of existing column types."
  [enrichable existing-enrichable]
  (let [paths (reduce (fn [new x] (conj new (:path x))) #{} (flatten (vals enrichable)))]
    (contains? paths existing-enrichable)))

(defn resolve-what-to-enrich
  "This is complex - we need to resolve which columns are of a suitable type to be enriched by a widget, and then select one. Where possible we try to always preserve the same column that the user was looking at in the previous enrichment, to ensure general consistency."
  [db]
  (let [query-parts (get-in db [:results :query-parts])
        widgets (get-in db [:assets :widgets (:current-mine db)])
        existing-enrichable-path (get-in db [:results :active-enrichment-column :path])
        existing-type (get-in db [:results :active-enrichment-column :type])
        enrichable (what-we-can-enrich widgets query-parts)
        use-existing-enrichable? (can-we-enrich-on-existing-preference? enrichable existing-enrichable-path)
        enrichable-default (last (last (vals enrichable)))]
    (if use-existing-enrichable?
      ;;default to the type we had selected last enrichment if possible
      (first (filter
              (fn [val] (= existing-enrichable-path (:path val))) (existing-type enrichable)))
      ;;otherwise just default to whatever
      enrichable-default)))

(reg-event-fx
 :enrichment/enrich
 (fn [{db :db} [_]]
   (let [query-parts (get-in db [:results :query-parts])
         widgets (get-in db [:assets :widgets (:current-mine db)])
         enrichable (what-we-can-enrich widgets query-parts)
         what-to-enrich (resolve-what-to-enrich db)
         can-enrich? (pos? (count enrichable))
         source-kw (get-in db [:results :package :source])]
     (if can-enrich?
       (let [enrich-query (:query what-to-enrich)]
         {:db (-> db
                  (assoc-in [:results :active-enrichment-column] what-to-enrich)
                  (assoc-in [:results :enrichable-columns] enrichable)
                  (assoc-in [:results :enrichment-results-loading?] true))
          :dispatch [:fetch-enrichment-ids-from-query
                     (get-in db [:mines source-kw :service])
                     enrich-query
                     what-to-enrich]})
       {:db db}))))

(reg-event-fx
 :enrichment/update-enrichment-setting
 (fn [{db :db} [_ setting value]]
   {:db (assoc-in db [:results :enrichment-settings setting] value)
    :dispatch [:enrichment/run-all-enrichment-queries]}))

(reg-event-fx
 :enrichment/update-widget-filter
 (fn [{db :db} [_ widget-kw value]]
   {:db (assoc-in db [:results :widget-filters widget-kw] value)
    :dispatch [:enrichment/run-one-enrichment-query widget-kw]}))

(defn widgets-to-map
  "When the web service gives us a vector, we make it into a map for easy lookup"
  [widgets]
  (reduce (fn [new-map vals]
            (assoc new-map (keyword (:name vals)) vals)) {} widgets))

(defn get-suitable-widgets
  "We only want to load widgets that can be used on our datatypes"
  [array-widgets classname]
  (let [widgets (widgets-to-map (filter #(= "enrichment" (:widgetType %)) array-widgets))]
    (if classname
      (into {} (filter
                (fn [[_ widget]] (contains? (set (:targets widget)) (name (:type classname)))) widgets))
      widgets)))

(defn build-enrichment-query
  "default enrichment query structure"
  [selection widget-name settings filters]
  [:enrichment/run
   (merge
    selection
    {:maxp 0.05
     :widget widget-name
     :correction "Holm-Bonferroni"}
    settings
    (when-let [filter-value (get filters (keyword widget-name))]
      {:filter filter-value}))])

(defn build-all-enrichment-queries
  "format all available widget types into queries"
  [selection suitable-widgets settings filters]
  (reduce (fn [new-vec [_ vals]]
            (conj new-vec (build-enrichment-query selection (:name vals) settings filters))) [] suitable-widgets))

(reg-event-fx
 :enrichment/run-all-enrichment-queries
 (fn [{db :db} [_]]
   (let [selection {:ids (get-in db [:results :ids-to-enrich])}
         settings (get-in db [:results :enrichment-settings])
         filters (get-in db [:results :widget-filters])
         widgets (get-in db [:assets :widgets (:current-mine db)])
         enrichment-column (get-in db [:results :active-enrichment-column])
         suitable-widgets (get-suitable-widgets widgets enrichment-column)
         queries (build-all-enrichment-queries selection suitable-widgets settings filters)]
     {:db (assoc-in db [:results :active-widgets] suitable-widgets)
      :dispatch-n queries})))

(reg-event-fx
 :enrichment/run-one-enrichment-query
 (fn [{db :db} [_ widget-kw]]
   (let [selection {:ids (get-in db [:results :ids-to-enrich])}
         settings (get-in db [:results :enrichment-settings])
         filters (get-in db [:results :widget-filters])
         query (build-enrichment-query selection (name widget-kw) settings filters)]
     {:dispatch query})))

(defn service [db mine-kw]
  (get-in db [:mines mine-kw :service]))

(reg-event-fx
 :enrichment/run
 (fn [{db :db} [_ params]]
   (let [updated-params (cond-> params
                                 ; Stringify our :ids coll to be comma separated
                          (contains? params :ids) (update :ids (partial string/join ",")))]
     (let [enrichment-chan (fetch/enrichment (service db (get-in db [:results :package :source])) updated-params)]
       {:db (assoc-in db [:results
                          :enrichment-results
                          (keyword (:widget updated-params))] nil)
        :im-chan {:chan enrichment-chan
                  :abort [:enrichment (:widget updated-params)]
                  :on-success [:enrichment/handle-results (:widget updated-params)]
                  :on-failure [:enrichment/handle-error (:widget updated-params)]}}))))

(reg-event-db
 :enrichment/handle-results
 (fn [db [_ widget-name results]]
   (-> db
       (assoc-in [:results :enrichment-results (keyword widget-name)] results)
       (assoc-in [:results :enrichment-results-loading?] false)
       ;; This will replace any preceeding messages from other enrichments.
       ;; That's fine, because it's only to show the message that's returned
       ;; when the list selected as background population contains other
       ;; items, which should be identical for all enrichment results.
       (assoc-in [:results :enrichment-results-message] (:message results)))))

(reg-event-db
 :enrichment/handle-error
 (fn [db [_ widget-name res]]
   (-> db
       (assoc-in [:results :enrichment-results (keyword widget-name)]
                 (or (get-in res [:body :error])
                     (str "Failed to get enrichment results for " widget-name)))
       (assoc-in [:results :enrichment-results-loading?] false))))

(reg-event-fx
 :enrichment/view-one-result
 (fn [{db :db} [_ details identifier]]
   (let [query (assoc (build-matches-query (:pathQuery details) (:pathConstraint details) identifier)
                      :title identifier)]
     {:dispatch [:results/history+
                 {:source (:current-mine db)
                  :type :query
                  :intent :enrichment
                  :value query}]})))

(reg-event-fx
 :enrichment/view-results
 (fn [{db :db} [_ details identifiers]]
   (let [query (assoc (build-matches-query (:pathQuery details) (:pathConstraint details) identifiers)
                      :title "Enrichment Results")]
     {:dispatch [:results/history+
                 {:source (:current-mine db)
                  :type :query
                  :intent :enrichment
                  :value query}]})))

(reg-event-fx
 :enrichment/download-results
 (fn [{db :db} [_ details identifiers]]
   (let [query (build-matches-query (:pathQueryForMatches details) (:pathConstraint details) identifiers)]
     {:im-chan {:chan (fetch/rows (service db (:current-mine db)) query)
                :on-success [:enrichment/download-results-enriched details identifiers]
                :on-failure [:enrichment/notify-failed-download (:title details)]}})))

(reg-event-fx
 :enrichment/notify-failed-download
 (fn [{db :db} [_ widget-title]]
   {:dispatch [:messages/add
               {:markup [:span (str "Failed to download results for " widget-title)]
                :style "danger"}]}))

(defn get-active-query-title [db]
  (let [query (get-in db [:results :queries
                          (get-in db [:results :history-index])])]
    (or (:display-title query)
        (get-in query [:value :title]))))

(reg-event-fx
 :enrichment/download-results-enriched
 (fn [{db :db} [_ widget-details selected-identifiers {:keys [results]}]]
   (if (empty? results)
     {:dispatch [:enrichment/notify-failed-download (:title widget-details)]}
     (let [selected-identifier? (set selected-identifiers)
           identifier->result (group-by first results)
           query-title (get-active-query-title db)]
       {:download-file
        {:filename (str query-title " " (:title widget-details) ".tsv")
         :filetype "tab-separated-values"
         :data (->> (:results widget-details)
                    (filter (comp selected-identifier? :identifier))
                    (map (fn [{:keys [identifier p-value description]}]
                           (string/join \tab [description p-value
                                              (string/join "," (->> identifier identifier->result (map second)))
                                              identifier])))
                    (string/join \newline))}}))))
