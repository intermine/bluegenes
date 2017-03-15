(ns redgenes.sections.results.events
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
       :dispatch-n [
                    [:enrichment/enrich]
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
       :dispatch-n [[:enrichment/enrich]
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
       :dispatch-n [[:enrichment/enrich]
                    [:im-tables.main/replace-all-state
                     [:results :fortable]
                     {:settings {:links {:vocab    {:mine "flymine"}
                                         :on-click (fn [val] (accountant/navigate! val))}}
                      :query    (get package :value)
                      :service  (get-in db [:mines (:source package) :service])}]]})))


(reg-event-fx
  :fetch-ids-from-query
  (fn [world [_ service query what-to-enrich]]
    {:im-chan {:chan (fetch/rows service query)
               :on-success [:success-fetch-ids]}}))

(reg-event-fx
  :success-fetch-ids
  (fn [{db :db} [_ results]]
    {:db       (assoc-in db [:results :ids-to-enrich] (flatten (:results results)))
     :dispatch [:enrichment/run-all-enrichment-queries]}))



(defn service [db mine-kw]
  (get-in db [:mines mine-kw :service]))

(reg-event-fx
  :results/run
  (fn [{db :db} [_ params]]
    (let [enrichment-chan (search/enrichment (service db (get-in db [:results :package :source])) params)]
      {:db                     (assoc-in db [:results
                                             :enrichment-results
                                             (keyword (:widget params))] nil)
       :enrichment/get-enrichment [(:widget params) enrichment-chan]})))
