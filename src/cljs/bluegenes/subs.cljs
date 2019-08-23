(ns bluegenes.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.pages.results.enrichment.subs]
            [clojure.string :refer [ends-with?]]
            [bluegenes.pages.querybuilder.subs]
            [bluegenes.components.search.subs]
            [bluegenes.subs.auth]
            [bluegenes.components.idresolver.subs]
            [lambdaisland.uri :refer [uri]]))

(reg-sub
 :name
 (fn [db]
   (:name db)))

(reg-sub
 :registry
 (fn [db]
   (:registry db)))

(defn clean-mine-url
  "Remove frivolous data from a mine url, primarily so we can compare them
  later on. Only the host and path are kept (protocol, trailing slash and
  others are removed), so that we only focus on the 'service' of the url."
  [u]
  (let [{:keys [host path]} (uri u)]
    (cond-> (str host path)
      (ends-with? path "/")
      (as-> s (subs s 0 (dec (count s)))))))

;; Sometimes we want the available mines from the registry, in addition to
;; whatever mine this bluegenes deployment belongs to (the :default mine).
;; This subscription merges the :default mine in with the registry.
;; (Handles case where :default mine is also present in registry.)
(reg-sub
 :registry-with-default
 :<- [:registry]
 :<- [:default-mine]
 (fn [[registry default-mine]]
   (let [default-url (get-in default-mine [:service :root])
         ;; This would be the namespace of the registry mine which is also set
         ;; as our default mine (or nil if there is no such thing).
         default-ns  (some (fn [[mine-ns mine-m]]
                             (when (= (clean-mine-url (:url mine-m))
                                      (clean-mine-url default-url))
                               mine-ns))
                           registry)]
     (if default-ns ; Whether our :default mine is part of the registry.
       ;; Rename it's key and namespace to :default
       (let [default-reg-mine (assoc (get registry default-ns) :namespace :default)]
         (-> registry
             (dissoc default-ns)
             (assoc :default default-reg-mine)))
       ;; Otherwise, add as own :default key.
       (let [default-mine-with-url (assoc default-mine :url default-url)]
         (assoc registry :default default-mine-with-url))))))

(reg-sub
 :short-name
 (fn [db]
   (:short-name db)))

(reg-sub
 :mine-url
 (fn [db]
   (let [mine-name (:current-mine db)
         url       (:url (:mine (mine-name (:mines db))))]
     (str "http://" url))))

(reg-sub :mine-default-organism
         (fn [db]
           (let [mine-name (:current-mine db)
                 organism  (:abbrev (mine-name (:mines db)))]
             organism)))

(reg-sub :mines
         (fn [db]
           (:mines db)))

(reg-sub
 :default-mine
 :<- [:mines]
 (fn [mines]
   (:default mines)))

(reg-sub
 :mine-name
 (fn [db] 7
   (:current-mine db)))

(reg-sub
 :current-mine-name
 (fn [db]
   (:current-mine db)))

(reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(reg-sub
 :panel-params
 (fn [db _]
   (:panel-params db)))

(reg-sub
 :app-db
 (fn [db _] db))

(reg-sub
 :who-am-i
 (fn [db _]
   (:who-am-i db)))

(reg-sub
 :fetching-report?
 (fn [db _]
   (:fetching-report? db)))

; TODO - This is used by the report page. There must be a better way.
(reg-sub
 :runnable-templates
 (fn [db _]
   (:templates (:report db))))

; TODO - This is used by the report page. There must be a better way.
(reg-sub
 :collections
 (fn [db _]
   (:collections (:report db))))

(reg-sub
 :model
 (fn [db _]
   (let [current-mine  (get-in db [:mines (get db :current-mine)])
         current-model (get-in current-mine [:service :model :classes])]
     current-model)))

(reg-sub
 :current-model
 (fn [db _]
   (let [current-mine (get-in db [:mines (get db :current-mine)])]
     (get-in current-mine [:service :model]))))

(reg-sub
 :lists
 (fn [db [_]]
   (get-in db [:assets :lists])))

(reg-sub
 :summary-fields
 (fn [db _]
   (:summary-fields (:assets db))))

(reg-sub
 :current-summary-fields
 (fn [db [_ class-kw]]
   (get-in db [:assets :summary-fields (:current-mine db)])))

; TODO - This is used by the report page. There must be a better way.
(reg-sub
 :report
 (fn [db _]
   (:report db)))

(reg-sub
 :progress-bar-percent
 (fn [db _]
   (:progress-bar-percent db)))

(reg-sub
 :cache/organisms
 (fn [db]
   (get-in db [:cache :organisms])))

(reg-sub
 :current-mine
 (fn [db]
   (get-in db [:mines (get db :current-mine)])))

(reg-sub
 :version
 (fn [db [_ mine-keyword]]
   (get-in db [:assets :intermine-version mine-keyword])))

(reg-sub
 :current-lists
 :<- [:lists]
 :<- [:current-mine-name]
 (fn [[all-lists current-mine-name]]
   (get all-lists current-mine-name)))

(reg-sub
 :current-possible-values
 :<- [:current-mine]
 (fn [current-mine [_ path]]
   (get-in current-mine [:possible-values path])))

(reg-sub
 :invalid-token?
 (fn [db]
   (get db :invalid-token?)))

(reg-sub
 :messages
 (fn [db]
   (sort-by :when > (vals (:messages db)))))
