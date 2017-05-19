(ns bluegenes.components.enrichment.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.filters :as filters]
            [imcljsold.search :as search]
            [imcljs.fetch :as fetch]
            [imcljs.path :as path]
            [clojure.spec :as s]
            [clojure.set :refer [intersection]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [bluegenes.interceptors :refer [clear-tooltips]]
            [dommy.core :refer-macros [sel sel1]]
            [bluegenes.sections.saveddata.events]
            [accountant.core :as accountant]
            [bluegenes.specs :as specs]
            [bluegenes.interceptors :refer [abort-spec]]))

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path   path-constraint
                   :op     "ONE OF"
                   :values [identifier]}))

(reg-event-fx
  :enrichment/get-item-details
  (fn [{db :db} [_ identifier path-constraint]]
    (let [source (get-in db [:results :package :source])
          model          (get-in db [:mines source :service :model])
          classname          (keyword (path/class model path-constraint))
          summary-fields (get-in db [:assets :summary-fields source classname])
          service (get-in db [:mines source :service])
          summary-chan   (search/raw-query-rows
                           service
                           {:from   classname
                            :select summary-fields
                            :where  [{:path  (last (clojure.string/split path-constraint "."))
                                      :op    "="
                                      :value identifier}]})]
      {:db                 (assoc-in db [:results :summary-chan] summary-chan)
       :get-summary-values summary-chan})))

(reg-event-fx
  :enrichment/set-text-filter
  (fn [{db :db} [_ value]]
    {:db (assoc-in db [:results :text-filter] value)}))

(reg-event-fx
  :enrichment/set-query
  (abort-spec specs/im-package)
  (fn [{db :db} [_ {:keys [source value type] :as package}]]
    (let [model (get-in db [:mines source :service :model :classes])]
      {:db         (update-in db [:results] assoc
                              :query value
                              :package package
                              ;:service (get-in db [:mines source :service])
                              :history [package]
                              :history-index 0
                              :query-parts (filters/get-parts model value)
                              :enrichment-results nil)
       ; TOOD ^:flush-dom
       :dispatch-n [[:enrichment/enrich]
                    [:im-tables.main/replace-all-state
                     [:results :fortable]
                     {:settings {:links {:vocab    {:mine (name source)}
                                         :on-click (fn [val] (accountant/navigate! val))}}
                      :query    value
                      :service  (get-in db [:mines source :service])}]]})))


(defn what-we-can-enrich
  "Returns list of columns in the results view that have widgets available for enrichment."
  [widgets query-parts]
  (let [possible-roots (set (keys query-parts))
        possible-enrichments (reduce (fn [x y] (conj x (keyword (first (:targets y))))) #{} widgets)
        enrichable-roots  (intersection possible-enrichments possible-roots)
        ]
        (select-keys query-parts enrichable-roots)
  ))

(reg-event-fx
 :enrichment/update-active-enrichment-column
  (fn [{db :db} [_ new-enrichment-column]]
    {:db (-> db
          (assoc-in [:results :active-enrichment-column] new-enrichment-column)
          ;;we need to remove the old results to prevent them showing up when a new
          ;;column setting has been selected. Fixes Issue #52.
          (update-in [:results] dissoc :enrichment-results)
          (assoc-in [:results :enrichment-results-loading?] true)
          )
     :dispatch [:enrichment/enrich]}
))

(defn can-we-enrich-on-existing-preference?
  "Returns whether or not the previously used enrichment column is within the list of existing column types."
  [enrichable existing-enrichable]
  (let [paths (reduce (fn [new x] (conj new (:path x))) #{} (flatten (vals enrichable)))]
    (contains? paths existing-enrichable)
  ))

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
                      (fn [val] (= existing-enrichable-path (:path val)))    (existing-type enrichable)))
            ;;otherwise just default to whatever
              enrichable-default
            )))

  (reg-event-fx
    :enrichment/enrich
    (fn [{db :db} [_ ]]
      (let [query-parts (get-in db [:results :query-parts])
            widgets (get-in db [:assets :widgets (:current-mine db)])
            enrichable (what-we-can-enrich widgets query-parts)
            what-to-enrich (resolve-what-to-enrich db)
            can-enrich?  (pos? (count enrichable))
            source-kw   (get-in db [:results :package :source])]
        (if can-enrich?
          (let [enrich-query (:query what-to-enrich)]
            {:db (-> db
                     (assoc-in [:results :active-enrichment-column] what-to-enrich)
                     (assoc-in [:results :enrichable-columns] enrichable))
             :dispatch [:fetch-ids-from-query (get-in db [:mines source-kw :service]) enrich-query what-to-enrich]})
          {:db db}))))

(reg-event-fx
  :enrichment/update-enrichment-setting
  (fn [{db :db} [_ setting value]]
    {:db       (assoc-in db [:results :enrichment-settings setting] value)
     :dispatch [:enrichment/run-all-enrichment-queries]}))

(defn widgets-to-map
  "When the web service gives us a vector, we make it into a map for easy lookup"
  [widgets]
  (reduce (fn [new-map  vals]
    (assoc new-map (keyword (:name vals)) vals)
) {} widgets))

(defn get-suitable-widgets
  "We only want to load widgets that can be used on our datatypes"
  [array-widgets classname]
  (let [widgets (widgets-to-map array-widgets)]
    (if classname
      (into {} (filter
        (fn [[_ widget]] (contains? (set (:targets widget)) (name (:type classname)))) widgets))
      widgets)
))

(defn build-enrichment-query [selection widget-name settings]
  "default enrichment query structure"
  [:enrichment/run
  (merge
    selection
    {:maxp 0.05
      :widget widget-name
      :correction "Holm-Bonferroni"}
      settings)])

(defn build-all-enrichment-queries [selection suitable-widgets settings]
  "format all available widget types into queries"
  (reduce (fn [new-vec [_ vals]]
    (conj new-vec (build-enrichment-query selection (:name vals) settings))
) [] suitable-widgets))

(reg-event-fx
  :enrichment/run-all-enrichment-queries
  (fn [{db :db} [_]]
    (let [selection {:ids (get-in db [:results :ids-to-enrich])}
          settings  (get-in db [:results :enrichment-settings])
          widgets (get-in db [:assets :widgets (:current-mine db)])
          enrichment-column (get-in db [:results :active-enrichment-column])
          suitable-widgets (get-suitable-widgets widgets enrichment-column)
          queries (build-all-enrichment-queries selection suitable-widgets settings)]
      {:db         (assoc-in db [:results :active-widgets] suitable-widgets)
       :dispatch-n queries})))


(defn service [db mine-kw]
  (get-in db [:mines mine-kw :service]))

(reg-event-fx
  :enrichment/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan (search/enrichment (service db (get-in db [:results :package :source])) params)]
      {:db                     (assoc-in db [:results
                                             :enrichment-results
                                             (keyword (:widget params))] nil)
       :enrichment/get-enrichment [(:widget params) enrichment-chan]})))

(reg-fx
  :enrichment/get-enrichment
  (fn [[widget-name results]]
    (go (dispatch [:enrichment/handle-results widget-name (<! results)]))))


(reg-event-db
  :enrichment/handle-results
  (fn [db [_ widget-name results]]
    (-> db
      (assoc-in [:results :enrichment-results (keyword widget-name)] results)
      (assoc-in [:results :enrichment-results-loading] false)
  )))
