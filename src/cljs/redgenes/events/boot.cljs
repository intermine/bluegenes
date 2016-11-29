(ns redgenes.events.boot
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [redgenes.db :as db]
            [redgenes.mines :as default-mines]
            [imcljs.fetch :as fetch]
            [redgenes.persistence :as persistence]))

(defn boot-flow [db]
  {:first-dispatch [:authentication/fetch-anonymous-token (get db :current-mine)]
   :rules          [
                    ; Fetch a token before anything else then load assets
                    {:when       :seen?
                     :events     :authentication/store-token
                     :dispatch-n [[:assets/fetch-model]
                                  [:assets/fetch-lists]
                                  [:assets/fetch-templates]
                                  [:assets/fetch-widgets]
                                  [:assets/fetch-summary-fields]
                                  [:assets/fetch-intermine-version]]}
                    ; When all assets are loaded let bluegenes know
                    {:when       :seen-all-of?
                     :events     [:assets/success-fetch-model
                                  :assets/success-fetch-lists
                                  :assets/success-fetch-templates
                                  :assets/success-fetch-summary-fields
                                  :assets/success-fetch-widgets
                                  :assets/success-fetch-intermine-version]
                     :dispatch-n (list [:finished-loading-assets] [:save-state])
                     :halt?      true}]})

(defn im-tables-events-forwarder []
  {:register    :im-tables-events ;;  <-- used
   :events      #{:imt.io/save-list-success}
   :dispatch-to [:assets/fetch-lists]})

; Boot the application.
(reg-event-fx
  :boot
  (fn []
    (let [db         (assoc db/default-db :mines default-mines/mines)
          state      (persistence/get-state!)
          has-state? (seq state)]
      (if has-state?
        {:db             (assoc db/default-db
                           :current-mine (:current-mine state)
                           :mines (:mines state)
                           :assets (:assets state)
                           ;;we had assets in localstorage. We'll still load the fresh ones in the background in case they changed, but we can make do with these for now.
                           :fetching-assets? false)
         :async-flow     (boot-flow db)
         :forward-events (im-tables-events-forwarder)}

        {:db             (assoc db/default-db
                           :mines default-mines/mines
                           :fetching-assets? true)
         :async-flow     (boot-flow db)
         :forward-events (im-tables-events-forwarder)})
      )))

(reg-event-fx
  :reboot
  (fn [{db :db}]
    {:db         db
     :async-flow (boot-flow db)}))

(reg-event-fx
  :finished-loading-assets
  (fn [{db :db}]
    {:db         (assoc db :fetching-assets? false)
     :dispatch-n [[:cache/fetch-organisms]
                  [:saved-data/load-lists]
                  [:regions/select-all-feature-types]]}))

; Store an authentication token for a given mine
(reg-event-db
  :authentication/store-token
  (fn [db [_ mine-kw token]]
    (assoc-in db [:mines mine-kw :service :token] token)))

; Fetch an anonymous token for a give
(reg-event-fx
  :authentication/fetch-anonymous-token
  (fn [{db :db} [_ mine-kw]]
    {:db           db
     :im-operation {:on-success [:authentication/store-token mine-kw]
                    :op         (partial fetch/session (get-in db [:mines mine-kw :service]))}}))

; Fetch model

(reg-event-db
  :assets/success-fetch-model
  (fn [db [_ mine-kw model]]
    (assoc-in db [:mines mine-kw :service :model] model)))

(reg-event-fx
  :assets/fetch-model
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/model (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-model (:current-mine db)]}}))

; Fetch lists

(reg-event-db
  :assets/success-fetch-lists
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :lists mine-kw] lists)))

(reg-event-fx
  :assets/fetch-lists
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/lists (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-lists (:current-mine db)]}}))

; Fetch templates

(reg-event-db
  :assets/success-fetch-templates
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :templates mine-kw] lists)))

(reg-event-fx
  :assets/fetch-templates
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/templates (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-templates (:current-mine db)]}}))

; Fetch summary fields

(reg-event-db
  :assets/success-fetch-summary-fields
  (fn [db [_ mine-kw lists]]
    (assoc-in db [:assets :summary-fields mine-kw] lists)))

(reg-event-fx
  :assets/fetch-summary-fields
  (fn [{db :db}]
    {:db           db
     :im-operation {:op         (partial fetch/summary-fields (get-in db [:mines (:current-mine db) :service]))
                    :on-success [:assets/success-fetch-summary-fields (:current-mine db)]}}))

(reg-event-fx
  :assets/fetch-widgets
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets are InterMine 1.x UI specific so are filtered out upon success
  (fn [{db :db}]
    {:im-operation
     {:op (partial fetch/widgets (get-in db [:mines (:current-mine db) :service]))
      :on-success [:assets/success-fetch-widgets (:current-mine db)]}}
    ))

(reg-event-db
  :assets/success-fetch-widgets
  (fn [db [_ mine-kw widgets]]
    (let [widget-type "enrichment"
          filtered-widgets (doall (filter (fn [widget]
      (= widget-type (:widgetType widget))
      ) (:widgets widgets)))]
    (assoc-in db [:assets :widgets mine-kw] filtered-widgets))))

(reg-event-fx
  :assets/fetch-intermine-version
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets are InterMine 1.x UI specific so are filtered out upon success
  (fn [{db :db}]
    {:im-operation
     {:op (partial fetch/version-intermine (get-in db [:mines (:current-mine db) :service]))
      :on-success [:assets/success-fetch-intermine-version (:current-mine db)]}}
    ))

(reg-event-db
  :assets/success-fetch-intermine-version
  (fn [db [_ mine-kw version]]
    (assoc-in db [:assets :intermine-version mine-kw] version)))
