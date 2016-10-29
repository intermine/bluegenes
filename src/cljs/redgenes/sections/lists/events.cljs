(ns redgenes.sections.lists.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as filters]
            [imcljs.search :as search]
            [clojure.spec :as s]
            [day8.re-frame.http-fx]
            [accountant.core :refer [navigate!]]
            [ajax.core :as ajax]
            [redgenes.interceptors :refer [clear-tooltips]]
            [dommy.core :refer-macros [sel sel1]]
            [redgenes.sections.saveddata.events]))

(reg-event-db
  :lists/set-text-filter
  (fn [db [_ value]]
    (let [adjusted-value (if (= value "") nil value)]
      (assoc-in db [:lists :controls :filters :text-filter] adjusted-value))))

(reg-event-db
  :lists/toggle-sort
  (fn [db [_ column-kw]]
    (update-in db [:lists :controls :sort column-kw]
               (fn [v]
                 (case v
                   :asc :desc
                   :desc nil
                   nil :asc)))))

(defn build-list-query [type summary-fields name title]
  {:title title
   :from   type
   :select summary-fields
   :where  [{:path  type
             :op    "IN"
             :value name}]})

(reg-fx
  :navigate
  (fn [url]
    (navigate! (str "#/" url))))

(reg-event-fx
  :lists/view-results
  (fn [{db :db} [_ {:keys [type name title]}]]
    (let [summary-fields (get-in db [:assets :summary-fields (keyword type)])]
      {:db       db
       :dispatch [:results/set-query (build-list-query type summary-fields name title)]
       :navigate (str "results")})))


(reg-event-db
  :lists/clear-filters
  (fn [db]
    (-> db
        (assoc-in [:lists :controls :filters :flags] {})
        (assoc-in [:lists :controls :filters :text-filter] nil))))

(reg-event-db
  :lists/toggle-flag-filter
  (fn [db [_ column-kw]]
    (update-in db [:lists :controls :filters :flags column-kw]
               (fn [v]
                 ; Tri-state toggle
                 ;(case v
                 ;  nil true
                 ;  true false
                 ;  false nil)
                 ; Bi-state toggle
                 (case v
                   nil true
                   true nil)))))