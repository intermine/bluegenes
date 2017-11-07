(ns bluegenes.sections.reportpage.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse select transform]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [imcljsold.filters :as filters]
            [imcljs.path :as path]))

(reg-event-db
  :handle-report-summary
  (fn [db [_ summary]]
    (-> db
        (assoc-in [:report :summary] summary)
        (assoc :fetching-report? false))))

(reg-event-fx
  :fetch-report
  (fn [{db :db} [_ mine type id]]
    (let [type-kw (keyword type)
          q       {:from type
                   :select (-> db :assets :summary-fields mine type-kw)
                   :where [{:path (str type ".id")
                            :op "="
                            :value id}]}]

      {:im-chan {:chan (fetch/rows (get-in db [:mines mine :service]) q {:format "json"})
                 :on-success [:handle-report-summary]}})))


(reg-event-db
  :filter-report-collections
  (fn [db [_ mine type oid]]
    (let [summary-fields (-> db :assets :summary-fields mine)
          type-key       (keyword type)
          collections    (-> db :mines mine :service :model :classes type-key :collections)]
      (assoc-in db [:report :collections]
                (map (fn [[_ {:keys [name referencedType]}]]
                       (let [summary-paths (-> referencedType keyword summary-fields)]
                         {:class referencedType
                          :service (get-in db [:mines mine :service])
                          :query {:from type
                                  :select (map (fn [path]
                                                 (str name "."
                                                      (clojure.string/join "."
                                                                           (drop 1 (clojure.string/split path ".")))))
                                               summary-paths)
                                  :where [{:op "="
                                           :path (str type ".id")
                                           :value oid}]}})) collections)))))

(defn when-n
  "Return a collection if its size is of n, otherwise nil"
  [n coll]
  (when (= n (count coll)) coll))

(reg-event-fx
  :filter-report-templates
  (fn [{db :db} [_ mine type id]]
    (let [model     (-> db :mines mine :service :model)
          templates (-> db :assets :templates mine)]
      {:db (assoc-in db [:report :templates]
                     (filter (fn [[template-kw {:keys [editable where] :as details}]]
                               (when-let [editable-constraint-path (->> where ; Get the query constraints
                                                                        (filter (comp true? :editable)) ; Filter for editable ones
                                                                        (when-n 1) ; Confirm there's only one
                                                                        first ; Take the first and only value in the coll
                                                                        :path)] ; And it's path
                                 (->> editable-constraint-path
                                      (path/trim-to-last-class model) ; Get the last class/property in the path "Gene.proteins.diseases.name => Diseases.name"
                                      (path/class model) ; Get the last class: :Disease
                                      name ; Get the string value Disease
                                      (= type)))) ; Compare it to our filter
                             templates))
       :dispatch [:filter-report-collections mine type id]})))

(reg-event-fx
  :load-report
  (fn [{db :db} [_ mine type id]]
    {:db (-> db
             (assoc :fetching-report? true)
             (dissoc :report))
     :dispatch-n [[:fetch-report (keyword mine) type id]
                  [:filter-report-templates (keyword mine) type id]]}))
