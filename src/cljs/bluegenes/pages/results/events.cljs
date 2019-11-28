(ns bluegenes.pages.results.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! close!]]
            [clojure.string :as s]
            [imcljs.fetch :as fetch]
            [imcljs.path :as path]
            [imcljs.query :as q]
            [imcljs.save :as save]
            [bluegenes.interceptors :refer [clear-tooltips]]
            [bluegenes.interceptors :refer [abort-spec]]
            [cljs-time.core :as time]
            [cljs-time.coerce :as time-coerce]
            [bluegenes.route :as route]
            [bluegenes.components.tools.events :as tools]))

(comment
  "To automatically display some results in this section (the Results / List Analysis page),
  fire the :results/history+ event with a package that represents a query, like so:"
  (dispatch [:results/history+ {:source :flymine
                                :type :query
                                :value {:title "Appears in Breadcrumb"
                                        :from "Gene"
                                        :select ["Gene.symbol"]
                                        :where {:path "Gene.symbol" :op "=" :value "runt"}}}])
  "
  Doing so will automatically direct the browser to the /results/[latest-history-index] route
  which in turn fires the [:results/load-history latest-history-index]. This triggers the fetching
  of enrichment results and boots the im-table
  ")

(defn build-matches-query [query path-constraint identifier]
  (update-in (js->clj (.parse js/JSON query) :keywordize-keys true) [:where]
             conj {:path path-constraint
                   :op "ONE OF"
                   :values [identifier]}))

(reg-event-db
 :save-summary-fields
 (fn [db [_ identifier response]]
   (assoc-in db [:results :summary-values identifier] response)))

(reg-fx
 :get-summary-values
 (fn [[identifier c]]
   (go (dispatch [:save-summary-fields identifier (<! c)]))))

(reg-event-fx
 :results/get-item-details
 (fn [{db :db} [_ identifier path-constraint]]
   (let [source (get-in db [:results :package :source])
         model (get-in db [:mines source :service :model])
         classname (keyword (path/class model path-constraint))
         summary-fields (get-in db [:assets :summary-fields source classname])
         service (get-in db [:mines source :service])
         summary-chan (fetch/rows
                       service
                       {:from classname
                        :select summary-fields
                        :where [{:path (last (clojure.string/split path-constraint "."))
                                 :op "="
                                 :value identifier}]})]
     {:db (assoc-in db [:results :summary-chan] summary-chan)
      :get-summary-values [identifier summary-chan]})))

; Fire this event to append a query package to the BlueGenes list analysis history
; and then route the browser to a URL that displays the last package in history (the one we just added)
(reg-event-fx
 :results/history+
 (abort-spec bluegenes.specs/im-package)
 (fn [{db :db} [_ {:keys [source value type] :as package} no-route?]]
   (cond-> {:db (-> db
                    (update-in [:results :history] conj package)
                    (assoc-in [:results :queries (:title value)]
                              (assoc package
                                     :last-executed
                                     (time-coerce/to-long (time/now)))))}
     (not no-route?)
           ;; Our route runs `:results/load-history`.
     (assoc :dispatch [::route/navigate ::route/list {:title (:title value)}]))))

; Load one package at a particular index from the list analysis history collection
(reg-event-fx
 :results/load-history
 [(clear-tooltips)] ; This clears any existing tooltips on the screen when the event fires
 (fn [{db :db} [_ title]]
   (let [; Get the details of the current package
         {:keys [source type value] :as package} (get-in db [:results :queries title])
          ; Get the current model
         model          (get-in db [:mines source :service :model])
         service        (get-in db [:mines source :service])
         summary-fields (get-in db [:assets :summary-fields source])]
     (if (nil? package)
       ;; The query result doesn't exist. Fail gracefully!
       (do
         (.error js/console
                 (str "[:results/load-history] The list titled " title " does not exist in db."))
         {})
       ; Store the values in app-db.
       ; TODO - 99% of this can be factored out by passing the package to the :enrichment/enrich and parsing it there
       {:db (update db :results assoc
                    :table nil
                    :query value
                    :package package
                    ; The index is used to highlight breadcrumbs
                    :history-index title
                    :query-parts (q/group-views-by-class model value)
                    ; Clear the enrichment results before loading any new ones
                    :enrichment-results nil)
        :dispatch-n [;; Fetch IDs to build tool entity, and then our tools.
                     [:fetch-ids-tool-entity]
                     ; Fire the enrichment event (see the TODO above)
                     [:enrichment/enrich]
                     [:im-tables/load
                      [:results :table]
                      {:service (merge service {:summary-fields summary-fields})
                       :query value
                       :settings {:pagination {:limit 10}
                                  :links {:vocab {:mine (name source)}
                                          :url (fn [{:keys [mine class objectId] :as vocab}]
                                                 (route/href ::route/report
                                                             {:mine mine
                                                              :type class
                                                              :id objectId}))}}}]]}))))

(reg-event-fx
 :fetch-ids-tool-entity
 (fn [{db :db} _]
   (let [{:keys [source value]} (get-in db [:results :package])
         service (get-in db [:mines source :service])
         query (assoc value :select ["Gene.id"])]
     {:im-chan {:chan (fetch/rows service query)
                :on-success [:success-fetch-ids-tool-entity]}})))

(reg-event-fx
 :success-fetch-ids-tool-entity
 (fn [{db :db} [_ {:keys [rootClass results]}]]
   (let [entity {:class rootClass
                 :format "ids"
                 :value (reduce into results)}]
     {:db (assoc-in db [:tools :entity] entity)
      :dispatch [::tools/fetch-tools]})))

(reg-event-fx
 :fetch-enrichment-ids-from-query
 (fn [world [_ service query what-to-enrich]]
   {:im-chan {:chan (fetch/rows service query)
              :on-success [:success-fetch-enrichment-ids-from-query]}}))

(reg-event-fx
 :success-fetch-enrichment-ids-from-query
 (fn [{db :db} [_ results]]
   {:db (assoc-in db [:results :ids-to-enrich] (flatten (:results results)))
    :dispatch [:enrichment/run-all-enrichment-queries]}))

(defn service [db mine-kw]
  (get-in db [:mines mine-kw :service]))

(reg-event-fx
 :results/run
 (fn [{db :db} [_ params]]
   (let [enrichment-chan (fetch/enrichment (service db (get-in db [:results :package :source])) params)]
     {:db (assoc-in db [:results
                        :enrichment-results
                        (keyword (:widget params))] nil)
      :enrichment/get-enrichment [(:widget params) enrichment-chan]})))

(reg-event-db
 :description/edit
 (fn [db [_ state]]
   (-> db
       (assoc-in [:results :description :editing?] state)
       (assoc-in [:results :errors :description] false))))

(reg-event-fx
 :description/update
 (fn [{db :db} [_ list-name new-description]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (save/im-list-update service list-name {:newDescription new-description})
                :on-success [:description/update-success]
                :on-failure [:description/update-failure]}})))

(reg-event-fx
 :description/update-success
 (fn [{db :db} [_ {:keys [listDescription listName]}]]
   {:dispatch [:description/edit false]
    :db (update-in db [:assets :lists (:current-mine db)]
                   (fn [lists]
                     (let [index (first (keep-indexed (fn [i {:keys [name]}]
                                                        (when (= name listName) i))
                                                      lists))]
                       (assoc-in lists [index :description] listDescription))))}))

(reg-event-db
 :description/update-failure
 (fn [db [_ {:keys [status body] :as res}]]
   (assoc-in db [:results :errors :description]
             (cond
               (string? body)
               ;; Server returned HTML as it didn't understand the request.
               "This feature is not supported in this version of InterMine."
               (= status 400)
               "You are not authorised to edit this description."
               :else
               (do (.error js/console "Failed imcljs request" res)
                   "An error occured. Please check your connection and try again.")))))
