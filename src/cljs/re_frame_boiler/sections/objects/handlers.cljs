(ns re-frame-boiler.sections.objects.handlers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse]])
  (:require [re-frame.core :as re-frame :refer [reg-event reg-event-fx reg-fx dispatch]]
            [re-frame-boiler.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.search :as search]
            [imcljs.filters :as filters]
            [com.rpl.specter :as s]))

(reg-event
  :handle-report-summary
  (fn [db [_ summary]]
    (assoc-in db [:report :summary] summary)))

(reg-fx
  :fetch-report
  (fn [[db type id]]
    (println "FETCHING REPORT")
    (let [type-kw (keyword type)
          q       {:from   type
                   :select (-> db :assets :summary-fields type-kw)
                   :where  {:id id}}]
      (go (dispatch [:handle-report-summary (<! (search/raw-query-rows
                                                  {:root "www.flymine.org/query"}
                                                  q
                                                  {:format "json"}))])))))

;(defn constraint-of-type? [constraint type]
;  (= type (filters/end-class model (:path constraint))))

(defn has-constraint-filter [model [id value]]
  (filter
    (fn [con]
      (filters/end-class model (:path con)))
    (:where value))
  true)

(defn filter-report-by-type-constraint [model templates]
  (filter #(has-constraint-filter model %) templates))

(reg-event
  :filter-report-templates
  (fn [db]
    (let [model     (-> db :assets :model)
          templates (-> db :assets :templates)]
      (assoc-in db [:report :templates]
                (into {} (traverse
                           [s/ALL
                            (s/selected? s/LAST :where #(= 1 (count (filter (fn [c] (:editable c)) %)))
                                         s/ALL :path #(= "Gene" (filters/end-class model %)))] templates))))))


(reg-event-fx
  :load-report
  (fn [{db :db} [_ type id]]
    {:db           (-> db
                       (assoc :fetching-report? true)
                       (dissoc :report))
     :fetch-report [db type id]
     :dispatch     [:filter-report-templates]}))


(def m {:k1 {:where [{:op    "="
                      :value "A"}
                     {:op    "ignore"
                      :value "X"}]}
        :k2 {:where [{:op    "<"
                      :value "A"}
                     {:op    "lookup"
                      :value "C"}]}
        :k3 {:where [{:op    "="
                      :value "A"}
                     {:op    "lookup"
                      :value "E"}]}})

#_(println "doene" (into {} (traverse [s/ALL (s/selected? s/LAST :where s/ALL :op #(= "=" %))] m)))

; result


