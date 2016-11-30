(ns redgenes.sections.results.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljsold.filters :as filters]
            [imcljsold.search :as search]
            [imcljs.fetch :as fetch]
            [clojure.spec :as s]
            [clojure.set :refer [intersection]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [redgenes.interceptors :refer [clear-tooltips]]
            [dommy.core :refer-macros [sel sel1]]
            [redgenes.sections.saveddata.events]
            [accountant.core :as accountant]
            [redgenes.interceptors :refer [abort-spec]]))

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path   path-constraint
                   :op     "ONE OF"
                   :values [identifier]}))

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
                           (get-in db [:results :service])
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
  (abort-spec redgenes.specs/im-package)
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
       :dispatch-n [[:results/enrich]
                    [:im-tables.main/replace-all-state
                     [:results :fortable]
                     {:settings {:links {:vocab    {:mine (name source)}
                                         :on-click (fn [val] (accountant/navigate! val))}}
                      :query    value
                      :service  (get-in db [:mines source :service])}]]})))


(reg-event-fx
  :results/add-to-history
  [(clear-tooltips)]
  (fn [{db :db} [_ {identifier :identifier} details]]
    (let [last-source (:source (last (get-in db [:results :history])))
          model       (get-in db [:mines last-source :service :model :classes])
          previous    (get-in db [:results :query])
          query       (merge (build-matches-query
                               (:pathQuery details)
                               (:pathConstraint details)
                               identifier)
                             {:title (str
                                       (:title details))})
          new-package {:source last-source
                       :type   :query
                       :value  query}]
      {:db         (-> db
                       (update-in [:results :history] conj new-package)
                       (update-in [:results] assoc
                                  :query query
                                  :package new-package
                                  :history-index (inc (get-in db [:results :history-index]))
                                  :query-parts (filters/get-parts model query)
                                  :enrichment-results nil))
       :dispatch-n [[:results/enrich]
                    [:im-tables.main/replace-all-state
                     [:results :fortable]
                     {:settings {:links {:vocab    {:mine "flymine"}
                                         :on-click (fn [val] (accountant/navigate! val))}}
                      :query    query
                      :service  (get-in db [:mines last-source :service])}]]})))

(reg-event-fx
  :results/load-from-history
  (fn [{db :db} [_ index]]
    (let [package (get-in db [:results :history index])
          model   (get-in db [:mines (:source package) :service :model :classes])]
      {:db         (-> db
                       (update-in [:results] assoc
                                  :query (get package :value)
                                  :package package
                                  :history-index index
                                  :query-parts (filters/get-parts model (get package :value))
                                  :enrichment-results nil))
       :dispatch-n [[:results/enrich]
                    [:im-tables.main/replace-all-state
                     [:results :fortable]
                     {:settings {:links {:vocab    {:mine "flymine"}
                                         :on-click (fn [val] (accountant/navigate! val))}}
                      :query    (get package :value)
                      :service  (get-in db [:mines (:source package) :service])}]]})))

(defn what-we-can-enrich [widgets query-parts]
  (.log js/console "%cquery parts" "color:cornflowerblue;" (clj->js query-parts))
  (let [possible-roots (set (keys query-parts))
        possible-enrichments (reduce (fn [x y] (conj x (keyword (first (:targets y))))) #{} widgets)]
          (apply sorted-set (intersection possible-enrichments possible-roots))
  ))

  (reg-event-fx
    :results/enrich
    (fn [{db :db} [_ enrich-root]]
      (let [query-parts (get-in db [:results :query-parts])
            widgets (get-in db [:assets :widgets (:current-mine db)])
            enrichable (what-we-can-enrich widgets query-parts)
            enrichable-default (first enrichable)
            can-enrich?  (pos? (count enrichable))
            source-kw   (get-in db [:results :package :source])]
            (.log js/console "%cenrichable" "color:orange;" (clj->js enrichable))

        (if can-enrich?
          (let [enrich-query (-> query-parts enrichable-default last :query)]
          (.log js/console "%cenrich-query" "color:mediumorchid;" (clj->js enrich-query) enrichable-default)
            (cond (> (count enrichable) 1) (.log js/console "%cThere's another enrichment option:" "color:cornflowerblue;" (clj->js enrichable)))
            {:db                   db
             :fetch-ids-from-query [(get-in db [:mines source-kw :service]) enrich-query enrichable-default]})
          {:db db}))))

(reg-event-fx
  :results/update-enrichment-setting
  (fn [{db :db} [_ setting value]]
    {:db       (assoc-in db [:results :enrichment-settings setting] value)
     :dispatch [:results/run-all-enrichment-queries]}))

(reg-fx
  :fetch-ids-from-query
  (fn [[service query classname]]
    (go (let [results  (<! (search/raw-query-rows
                                       service
                                       query))]
          (.log js/console "%cresults" "color:firebrick;" (clj->js results))
          (dispatch [:success-fetch-ids (flatten (:results results)) classname])))))

(reg-event-fx
  :success-fetch-ids
  (fn [{db :db} [_ results classname]]
    {:db       (assoc-in db [:results :ids-to-enrich] results)
     :dispatch [:results/run-all-enrichment-queries classname]}))


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
  (println classname (name classname))
    (if classname
      (into {} (filter
        (fn [[_ widget]] (contains? (set (:targets widget)) (name classname))) widgets))
      widgets)
))

(defn build-enrichment-query [selection widget-name settings]
  "default enrichment query structure"
  [:results/run
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
  :results/run-all-enrichment-queries
  (fn [{db :db} [_ classname]]
    (let [selection {:ids (get-in db [:results :ids-to-enrich])}
          settings  (get-in db [:results :enrichment-settings])
          widgets (get-in db [:assets :widgets (:current-mine db)])
          suitable-widgets (get-suitable-widgets widgets classname)
          queries (build-all-enrichment-queries selection suitable-widgets settings)]
          (.log js/console "%cenriching" "color:darkseagreen;" "selection" (clj->js selection) "widgets" widgets suitable-widgets "query" (clj->js queries))

      {:db         (assoc-in db [:results :active-widgets] suitable-widgets)
       :dispatch-n queries})))


(defn service [db mine-kw]
  (get-in db [:mines mine-kw :service]))

(reg-event-fx
  :results/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan (search/enrichment (service db (get-in db [:results :package :source])) params)]
      {:db                     (assoc-in db [:results
                                             :enrichment-results
                                             (keyword (:widget params))] nil)
       :results/get-enrichment [(:widget params) enrichment-chan]})))

(reg-fx
  :results/get-enrichment
  (fn [[widget-name results]]
    (go (dispatch [:results/handle-results widget-name (<! results)]))))


(reg-event-db
  :results/handle-results
  (fn [db [_ widget-name results]]
    (assoc-in db [:results :enrichment-results (keyword widget-name)] results)))
