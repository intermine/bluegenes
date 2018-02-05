(ns bluegenes.components.templates.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [re-frame.events]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]))


(def ns->kw (comp keyword namespace))
(def name->kw (comp keyword name))
(def ns->vec (juxt ns->kw name->kw))

; Predictable function used to filter active constraints
(def not-disabled-predicate (comp (partial not= "OFF") :switched))

(defn remove-switchedoff-constraints
  "Filter the constraints of a query map and only keep those with a :switched value other than OFF"
  [query]
  (update query :where #(filter not-disabled-predicate %)))

(reg-event-fx
  :template-chooser/choose-template
  (fn [{db :db} [_ id]]
    (let [query (get-in db [:assets :templates (ns->kw id) (name->kw id)])]
      {:db (update-in db [:components :template-chooser]
                      assoc
                      :selected-template query
                      :selected-template-name id
                      :selected-template-service (get-in db [:mines (ns->kw id) :service])
                      :count nil)
       :dispatch-n [[:template-chooser/run-count]
                    [:template-chooser/fetch-preview]]
       })))

(reg-event-db
  :template-chooser/set-category-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :selected-template-category] id)))

(reg-event-db
  :template-chooser/set-text-filter
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :text-filter] id)))


(reg-event-fx
  :templates/send-off-query
  (fn [{db :db} [_]]
    (let [summary-fields (get-in db [:assets :summary-fields (keyword type)])]
      {:db db
       :dispatch [:results/history+
                  {:source (ns->kw (get-in db [:components :template-chooser :selected-template-name]))
                   :type :query
                   :value (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))}]})))

(defn one-of? [col value] (some? (some #{value} col)))
(defn should-update? [old-op new-op]
  (or
    (and
      (one-of? ["IN" "NOT IN"] old-op)
      (one-of? ["IN" "NOT IN"] new-op))
    (and (not (one-of? ["IN" "NOT IN"] old-op)) (not (one-of? ["IN" "NOT IN"] new-op)))))

(reg-event-fx
  :template-chooser/replace-constraint
  (fn [{db :db} [_ index new-constraint]]
    (let [constraint-location [:components :template-chooser :selected-template :where index]
          old-constraint (get-in db constraint-location)]
      ; Only fetch the query results if the operator hasn't change from a LIST to a VALUE or vice versa
      (if (should-update? (:op old-constraint) (:op new-constraint))
        {:db (assoc-in db constraint-location new-constraint)
         :dispatch-n [[:template-chooser/run-count]
                      [:template-chooser/fetch-preview]]}
        {:db (-> db
                 (assoc-in constraint-location (assoc new-constraint :value nil))
                 (assoc-in [:components :template-chooser :results-preview] nil))}))))

(reg-event-db
  :template-chooser/update-count
  (fn [db [_ c]]
    (update-in db [:components :template-chooser] assoc
               :count c
               :counting? false)))




(reg-event-db
  :template-chooser/store-results-preview
  (fn [db [_ results]]
    (update-in db [:components :template-chooser] assoc
               :results-preview results
               :fetching-preview? false)))

(reg-fx
  :template-chooser/pipe-preview
  (fn [preview-chan]
    (go (dispatch [:template-chooser/store-results-preview (<! preview-chan)]))))

(reg-event-fx
  :template-chooser/fetch-preview
  (fn [{db :db}]
    (let [query (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))
          template-name (get-in db [:components :template-chooser :selected-template-name])
          service (get-in db [:mines (ns->kw template-name) :service])
          count-chan (fetch/table-rows service query {:size 5})
          new-db (update-in db [:components :template-chooser] assoc
                            :preview-chan count-chan
                            :fetching-preview? true)]
      {:db new-db
       :im-chan {:chan count-chan
                 :on-success [:template-chooser/store-results-preview]}})))



(reg-fx
  :template-chooser/pipe-count
  (fn [count-chan]
    (go (dispatch [:template-chooser/update-count (<! count-chan)]))))

(reg-event-fx
  :template-chooser/run-count
  (fn [{db :db}]
    (let [query (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))
          template-name (get-in db [:components :template-chooser :selected-template-name])
          service (get-in db [:mines (ns->kw template-name) :service])
          count-chan (fetch/row-count
                       service
                       query)
          new-db (update-in db [:components :template-chooser] assoc
                            :count-chan count-chan
                            :counting? true)]
      {:db new-db
       :template-chooser/pipe-count count-chan})))
