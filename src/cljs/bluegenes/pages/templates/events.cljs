(ns bluegenes.pages.templates.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [re-frame.events]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [bluegenes.route :as route]
            [bluegenes.components.ui.constraint :as constraint]))

;; This effect handler is used from routes and has different behaviour
;; depending on if it's called from a different panel, or the template panel.
(reg-event-fx
 :template-chooser/open-template
 (fn [{db :db} [_ id]]
   (if (= :templates-panel (:active-panel db))
     {:dispatch [:template-chooser/choose-template id]}
     {:dispatch-n [[:template-chooser/clear-template]
                   [:set-active-panel :templates-panel
                    nil
                    ;; flush-dom makes the event wait for the page to update first.
                    ;; This is because we'll be scrolling to the template, so the
                    ;; element needs to be present first.
                    ^:flush-dom [:template-chooser/choose-template id
                                 {:scroll? true}]]]})))

; Predictable function used to filter active constraints
(def not-disabled-predicate (comp (partial not= "OFF") :switched))

(defn remove-switchedoff-constraints
  "Filter the constraints of a query map and only keep those with a :switched value other than OFF"
  [query]
  (update query :where #(filterv not-disabled-predicate %)))

(reg-event-fx
 :template-chooser/choose-template
 (fn [{db :db} [_ id {:keys [scroll?] :as _opts}]]
   (let [current-mine (:current-mine db)
         query (get-in db [:assets :templates current-mine id])]
     (if (not-empty query)
       (merge
        {:db (update-in db [:components :template-chooser] assoc
                        :selected-template query
                        :selected-template-name id
                        :selected-template-service (get-in db [:mines current-mine :service])
                        :count nil
                        :results-preview nil)
         :dispatch-n [[:template-chooser/run-count]
                      [:template-chooser/fetch-preview]]}
        (when scroll?
          {:scroll-to-template (name id)}))
       ;; Template can't be found.
       {:dispatch-n [[::route/navigate ::route/templates]
                     [:messages/add
                      {:markup [:span "The template " [:em (name id)] " does not exist. It's possible the ID has changed. Use the text filter above to find a template with a similar name."]
                       :style "warning"
                       :timeout 0}]]}))))

(reg-event-db
 :template-chooser/deselect-template
 (fn [db [_]]
   (update-in db [:components :template-chooser] select-keys
              [:selected-template-category :text-filter])))
;; Above keeps category and text filter, while the below clears them.
(reg-event-db
 :template-chooser/clear-template
 (fn [db [_]]
   (update db :components dissoc :template-chooser)))

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
   {:db db
    :dispatch [:results/history+
               {:source (:current-mine db)
                :type :query
                :intent :template
                :value (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))}]}))

(reg-event-fx
 :templates/edit-query
 (fn [{db :db} [_]]
   {:db db
    :dispatch-n [[::route/navigate ::route/querybuilder]
                 [:qb/load-query (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))]]}))

(reg-event-fx
 :template-chooser/replace-constraint
 (fn [{db :db} [_ index {new-op :op :as new-constraint}]]
   (let [constraint-location [:components :template-chooser :selected-template :where index]
         {old-op :op :as old-constraint} (get-in db constraint-location)]
     {:db (-> db
              (assoc-in constraint-location (constraint/clear-constraint-value old-constraint new-constraint))
              (cond->
                (not= old-op new-op) (assoc-in [:components :template-chooser :results-preview] nil)))})))

(reg-event-fx
 :template-chooser/update-preview
 (fn [{db :db} [_ _index new-constraint]]
   (if (constraint/satisfied-constraint? new-constraint)
     {:dispatch-n [[:template-chooser/run-count]
                   [:template-chooser/fetch-preview]]}
     {:db (assoc-in db [:components :template-chooser :results-preview] nil)})))

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
         service (get-in db [:mines (:current-mine db) :service])
         count-chan (fetch/table-rows service query {:size 5})
         query-changed? (not= query (get-in db [:components :template-chooser :previously-ran]))
         new-db (update-in db [:components :template-chooser] assoc
                           :preview-chan count-chan
                           :previously-ran query)]
     (cond-> {:db new-db}
       query-changed? (-> (update-in [:db :components :template-chooser] assoc
                                     :fetching-preview? true
                                     :preview-error nil)
                          (assoc :im-chan {:chan count-chan
                                           :on-success [:template-chooser/store-results-preview]
                                           :on-failure [:template-chooser/fetch-preview-failure]}))))))

(reg-event-db
 :template-chooser/fetch-preview-failure
 (fn [db [_ res]]
   (update-in db [:components :template-chooser] assoc
              :fetching-preview? false
              :preview-error (or (get-in res [:body :error])
                                 "Error occurred when running template."))))

(reg-fx
 :template-chooser/pipe-count
 (fn [count-chan]
   (go (dispatch [:template-chooser/update-count (<! count-chan)]))))

(reg-event-fx
 :template-chooser/run-count
 (fn [{db :db}]
   (let [query (remove-switchedoff-constraints (get-in db [:components :template-chooser :selected-template]))
         service (get-in db [:mines (:current-mine db) :service])
         count-chan (fetch/row-count service query)
         new-db (update-in db [:components :template-chooser] assoc
                           :count-chan count-chan
                           :counting? true)]
     {:db new-db
      :template-chooser/pipe-count count-chan})))
