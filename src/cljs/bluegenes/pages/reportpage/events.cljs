(ns bluegenes.pages.reportpage.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx dispatch]]
            [imcljs.fetch :as fetch]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :refer [document-title]]
            [clojure.string :as string]
            [clojure.set :as set]
            [bluegenes.pages.reportpage.utils :as utils]
            [bluegenes.route :as route]
            [goog.dom :as gdom]
            [oops.core :refer [oget ocall]]
            [bluegenes.utils :refer [rows->maps compatible-version?]]))

(defn views->results
  "Convert a `fetch/rows` response map into a vector of maps from keys
  corresponding to the tail of the view, and values being the pair of the key.
  Ex. [{:name 'BioGRID interaction data set' :url 'http://www.thebiogrid.org/downloads.php'}
       {:name 'Panther orthologue and paralogue predictions' :url 'http://pantherdb.org/'}]"
  [{:keys [views results] :as _res}]
  (let [concise-views (map #(-> % (string/split #"\.") last keyword)
                           views)]
    (mapv (partial zipmap concise-views)
          results)))

(reg-event-fx
 :fetch-fasta
 (fn [{db :db} [_ mine-kw type id]]
   (let [service (get-in db [:mines mine-kw :service])
         q {:from type
            :select [(str type ".id")]
            :where [{:path (str type ".id")
                     :op "="
                     :value id}]}]
     {:im-chan {:chan (fetch/fasta service q)
                :on-success [:handle-fasta]}})))

(reg-event-db
 :handle-fasta
 (fn [db [_ fasta]]
   (cond-> db
     ;; If there's no FASTA the API will return "Nothing was found for export".
     (string/starts-with? fasta ">")
     (assoc-in [:report :fasta] fasta))))

(defn class-has-fasta? [hier class-kw]
  (or (= class-kw :Protein)
      (isa? hier class-kw :SequenceFeature)))

(defn fasta-too-long? [summary]
  (let [length (get-in (rows->maps summary) [0 :length])]
    (if (number? length)
      (> length 1e6)
      true))) ; There's likely no FASTA available if it has no length.

(defn lookup-value
  "Takes a gene summary response and returns the value best used for LOOKUP query."
  [summary]
  (let [results (first (views->results summary))]
    (some results [:primaryIdentifier :secondaryIdentifier :symbol])))

(reg-event-fx
 :handle-report-summary
 [document-title]
 (fn [{db :db} [_ mine-kw type id summary]]
   (if (seq (:results summary))
     (let [has-fasta (class-has-fasta? (get-in db [:mines mine-kw :model-hier]) (keyword type))
           too-long-fasta (and has-fasta (fasta-too-long? summary))]
       {:db (-> db
                (assoc-in [:report :summary] summary)
                (assoc-in [:report :title] (utils/title-column summary))
                (assoc-in [:report :active-toc] utils/pre-section-id)
                (cond-> too-long-fasta
                  (assoc-in [:report :fasta] :too-long))
                (assoc :fetching-report? false))
        :dispatch-n [[:viz/run-queries]
                     (when (and has-fasta (not too-long-fasta))
                       [:fetch-fasta mine-kw type id])
                     (when (= type "Gene")
                       [::fetch-homologues mine-kw (lookup-value summary)])]})
     ;; No results mean the object ID likely doesn't exist.
     {:db (-> db
              (assoc-in [:report :error] {:type :not-found})
              (assoc :fetching-report? false))})))

(reg-event-db
 :fetch-report-failure
 (fn [db [_ res]]
   (-> db
       (assoc-in [:report :error] {:type :ws-failure
                                   :message (get-in res [:body :error])})
       (assoc :fetching-report? false))))

(reg-event-fx
 :fetch-report
 (fn [{db :db} [_ mine-kw type id]]
   (let [service (get-in db [:mines mine-kw :service])
         attribs (->> (get-in service [:model :classes (keyword type) :attributes])
                      (mapv (comp #(str type "." %) :name val)))
         q       {:from type
                  :select attribs
                  :where [{:path (str type ".id")
                           :op "="
                           :value id}]}]
     {:im-chan {:chan (fetch/rows service q {:format "json"})
                :on-success [:handle-report-summary mine-kw type id]
                :on-failure [:fetch-report-failure]}})))

(defn class-has-dataset? [model-classes class-kw]
  (contains? (get-in model-classes [class-kw :collections])
             :dataSets))

(reg-event-fx
 :load-report
 (fn [{db :db} [_ mine type id]]
   (let [entity {:class type
                 :format "id"
                 :value id}]
     {:db (-> db
              (assoc :fetching-report? true)
              (dissoc :report)
              (assoc-in [:tools :entities (keyword type)] entity))
      :dispatch-n [[:fetch-report (keyword mine) type id]
                   [::fetch-lists (keyword mine) id]
                   (when (compatible-version? "5.0.0" (get-in db [:assets :intermine-version (keyword mine)]))
                     [::fetch-external-links (keyword mine) id])
                   (when (class-has-dataset? (get-in db [:mines (keyword mine) :service :model :classes])
                                             (keyword type))
                     [::fetch-sources (keyword mine) type id])]})))

(reg-event-fx
 ::fetch-lists
 (fn [{db :db} [_ mine-kw id]]
   (let [service (get-in db [:mines mine-kw :service])]
     {:im-chan {:chan (fetch/lists-containing service {:id id})
                :on-success [::handle-lists]}})))

(reg-event-db
 ::handle-lists
 (fn [db [_ lists]]
   (assoc-in db [:report :lists] lists)))

(reg-event-fx
 ::fetch-sources
 (fn [{db :db} [_ mine-kw class id]]
   (let [service (get-in db [:mines mine-kw :service])
         q {:from class
            :select ["dataSets.description"
                     "dataSets.url"
                     "dataSets.name"
                     "dataSets.id"]
            :where [{:path (str class ".id")
                     :op "="
                     :value id}]}]
     {:im-chan {:chan (fetch/rows service q {:format "json"})
                :on-success [::handle-sources]}})))

(reg-event-db
 ::handle-sources
 (fn [db [_ res]]
   (assoc-in db [:report :sources] (views->results res))))

(reg-event-fx
 ::open-in-region-search
 (fn [{db :db} [_ chromosome-location]]
   {:dispatch-n [[::route/navigate ::route/regions]
                 [:regions/set-to-search chromosome-location]
                 [:regions/run-query]]}))

(reg-event-db
 ::set-active-toc
 (fn [db [_ active-toc-id]]
   (assoc-in db [:report :active-toc] active-toc-id)))

(reg-event-db
 ::set-filter-text
 (fn [db [_ text]]
   (assoc-in db [:report :filter-text] text)))

(defn scrollspy
  "Returns a function (partially applied with a seq of string element IDs) to
  be called on document scroll. Will go through every element until it finds
  one that hasn't been scrolled passed, then dispatches an event with its ID."
  [ids]
  ;; Predefined top section is always initial value.
  (let [last-scrolled-id* (atom utils/pre-section-id)]
    (fn [_evt]
      (loop [ids ids]
        (when (seq ids)
          (let [id (first ids)
                ;; An element can be nil in the following scenarios
                ;; - it's a category with a collapsed parent section
                ;; - it's a category not "available" for the active class (usually from default layout)
                ;; - it's a section with no children (and therefore not shown)
                ;; in which case we should skip to the next ID.
                top (some-> (gdom/getElement id)
                            (ocall :getBoundingClientRect)
                            (oget :top))]
            (if (pos? ^number top) ; We'll get a nil warning without the typehint.
              (when (not= id @last-scrolled-id*)
                (reset! last-scrolled-id* id)
                (dispatch [::set-active-toc id]))
              (recur (rest ids)))))))))

(defn categories->ids
  "Returns a seq of all the IDs of sections/categories and their children, in
  order of placement from top to bottom."
  [cats]
  (concat [(symbol utils/pre-section-id)] ; Predefined section on the report page.
          (mapcat #(cons (:id %) (map :id (:children %))) cats)))

;; Working around crappy DOM API... (we have to create an anonymous function to
;; pass arguments to the listener, and keep a reference to that so we can
;; remove it later)
(defonce scrollspy-fn* (atom nil))

(reg-event-fx
 ::start-scroll-handling
 (fn [{db :db} [_ categories]]
   (let [ids (map str (categories->ids categories))]
     (.addEventListener js/window "scroll" (reset! scrollspy-fn* (scrollspy ids)))
     {})))

(reg-event-fx
 ::stop-scroll-handling
 (fn [_ [_]]
   (.removeEventListener js/window "scroll" @scrollspy-fn*)
   {}))

(reg-event-fx
 ::generate-permanent-url
 (fn [{db :db} [_ api-version]]
   (if (>= api-version 30)
     (let [{:keys [id type]} (:panel-params db)
           service (get-in db [:mines (:current-mine db) :service])
           ?options (when (= api-version 30)
                      {:type type})]
       {:db (assoc-in db [:report :share] nil)
        :im-chan {:chan (fetch/permanent-url service id ?options)
                  :on-success [::show-permanent-url]
                  :on-failure [::show-permanent-url-error]}})
     {:dispatch [::show-permanent-url-error
                 {:body {:error (str "This feature isn't supported on " (name (:current-mine db)) " due to using an InterMine API version below 30.")}}]})))

(reg-event-db
 ::show-permanent-url
 (fn [db [_ url]]
   (assoc-in db [:report :share] {:status :success
                                  :url url})))

(reg-event-db
 ::show-permanent-url-error
 (fn [db [_ res]]
   (assoc-in db [:report :share] {:status :failure
                                  :error (get-in res [:body :error])})))

(reg-event-fx
 ::fetch-external-links
 (fn [{db :db} [_ mine-kw id]]
   (let [service (get-in db [:mines mine-kw :service])]
     {:im-chan {:chan (fetch/external-links service id)
                :on-success [::handle-external-links]}})))

(reg-event-db
 ::handle-external-links
 (fn [db [_ links]]
   (assoc-in db [:report :external-links] links)))

(defn env->registry
  "Adds keys where they're expected for a registry mine to a configured mine."
  [env-mine]
  (assoc env-mine
         :namespace (-> env-mine :id name)
         :url (-> env-mine :service :root)))

(defn shim-homologue-path
  [mine-namespace]
  (case mine-namespace
    "phytomine" "homolog.gene"
    "homologues.homologue"))

(defn homologue-query
  [gene-name {mine-ns :namespace :as _mine}]
  {:from "Gene"
   :select ["id"
            "symbol"
            "primaryIdentifier"
            "secondaryIdentifier"
            "organism.shortName"]
   :where [{:path (shim-homologue-path mine-ns)
            :op "LOOKUP"
            :value gene-name}]})

(reg-event-fx
 ::fetch-homologues
 (fn [{db :db} [_ mine-kw gene-name]]
   (let [env-mines (into {} (map #(update % 1 env->registry)
                                 (get-in db [:env :mines])))
         registry-mines (get db :registry)
         neighbourhood (set (get-in registry-mines [mine-kw :neighbours]))
         neighbour-mines (if (empty? neighbourhood)
                           env-mines
                           (merge
                            (into {}
                                  (remove (fn [[_ {:keys [neighbours]}]]
                                            (empty? (set/intersection neighbourhood (set neighbours))))
                                          registry-mines))
                            env-mines))]
     {:dispatch-n (for [mine (-> neighbour-mines (dissoc mine-kw) vals)]
                    [::fetch-mine-homologues
                     {:root (:url mine)
                      :model {:name "genomic"}}
                     (homologue-query gene-name mine)
                     mine])})))

(defn read-mine-kw [mine]
  (-> mine :namespace keyword))

(reg-event-fx
 ::fetch-mine-homologues
 (fn [{db :db} [_ service query mine]]
   {:db (assoc-in db [:report :homologues (read-mine-kw mine)]
                  {:mine mine
                   :loading? true})
    :im-chan {:chan (fetch/rows service query {:format "json"})
              :on-success [::handle-mine-homologues mine]
              :on-failure [::handle-mine-homologues-failure mine]}}))

(reg-event-db
 ::handle-mine-homologues
 (fn [db [_ mine res]]
   (assoc-in db [:report :homologues (read-mine-kw mine)]
             {:mine mine
              :homologues (when (seq (:results res))
                            (group-by :shortName (views->results res)))})))

(reg-event-db
 ::handle-mine-homologues-failure
 (fn [db [_ mine res]]
   (assoc-in db [:report :homologues (read-mine-kw mine)]
             {:mine mine
              :error (or (get-in res [:body :error])
                         "Error response to query")})))
