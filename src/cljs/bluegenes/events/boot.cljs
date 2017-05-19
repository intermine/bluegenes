(ns bluegenes.events.boot
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.db :as db]
            [bluegenes.mines :as default-mines]
            [imcljs.fetch :as fetch]
            [bluegenes.persistence :as persistence]))

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
  {:register    :im-tables-events                           ;;  <-- used
   :events      #{:imt.io/save-list-success}
   :dispatch-to [:assets/fetch-lists]})

(defn get-current-mines
  "This method is implemented for robust updates. It ensures that local-storage client-cached mine entries are deleted if the mine entry is removed from mines.cljc. Goes hand in hand with get-active mine to ensure that we still have an active mine to select"
  [state-mines config-mines]
  (let [good-mines (set (keys config-mines))]
    (doall
      (reduce
        (fn [new-mine-list [mine-name details]]
          (if (contains? good-mines mine-name)
            (assoc new-mine-list mine-name details))) {} state-mines))
    ))

(defn get-active-mine "Return the current mine if it still exists after a config update, or else just return the first one if the ID doesn't exist for some reason"
  [all-mines mine-name]
  (let [mine-names (set (keys all-mines))]
    (if (contains? mine-names mine-name)
      mine-name
      (do
        (.debug js/console (clj->js mine-name) "doesn't exist so we've auto-selected the first available mine")
        (first mine-names)))
    ))

; Boot the application.
(reg-event-fx
  :boot
  (fn []
    (let [db               (assoc db/default-db :mines default-mines/mines)
          state            (persistence/get-state!)
          has-state?       (seq state)
          ;;prune out old mines from localstorage that aren't part of the app anymore
          good-state-mines (get-current-mines (:mines state) (:mines db))
          ;;make sure we have all current localstorage mines and all new ones (if any)
          all-mines        (merge default-mines/mines good-state-mines)
          ;;make sure the active mine wasn't removed. Will select a default if needed.
          current-mine     (get-active-mine all-mines (:current-mine state))]

      ; Do not use data from local storage if the client version in local storage
      ; is not the same as the current client version
      (if (and has-state? (= bluegenes.core/version (:version state)))
        {:db             (assoc db/default-db
                           :current-mine current-mine
                           :mines all-mines
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

(defn remove-stateful-keys-from-db
  "Any tools / components that have mine-specific state should lose that state if we switch mines. For example, in list upload (ID Resolver), drosophila IDs are no longer valid when using humanmine."
  [db]
  (dissoc db :regions :idresolver :results :qb))

(reg-event-fx
  :reboot
  (fn [{db :db}]
    {:db         (remove-stateful-keys-from-db db)
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

; Fetch an anonymous token for a given mine
(reg-event-fx
  :authentication/fetch-anonymous-token
  (fn [{db :db} [_ mine-kw]]
    (let [mine (dissoc (get-in db [:mines mine-kw :service]) :token)]
      {:db           db
       :im-operation {:on-success [:authentication/store-token mine-kw]
                      :op         (partial fetch/session mine)}})))

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
     {:op         (partial fetch/widgets (get-in db [:mines (:current-mine db) :service]))
      :on-success [:assets/success-fetch-widgets (:current-mine db)]}}
    ))

(reg-event-db
  :assets/success-fetch-widgets
  (fn [db [_ mine-kw widgets]]
    (let [widget-type      "enrichment"
          filtered-widgets (doall (filter (fn [widget]
                                            (= widget-type (:widgetType widget))
                                            ) (:widgets widgets)))]
      (assoc-in db [:assets :widgets mine-kw] filtered-widgets))))

(reg-event-fx
  :assets/fetch-intermine-version
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets are InterMine 1.x UI specific so are filtered out upon success
  (fn [{db :db}]
    {:im-operation
     {:op         (partial fetch/version-intermine (get-in db [:mines (:current-mine db) :service]))
      :on-success [:assets/success-fetch-intermine-version (:current-mine db)]}}
    ))

(reg-event-db
  :assets/success-fetch-intermine-version
  (fn [db [_ mine-kw version]]
    (assoc-in db [:assets :intermine-version mine-kw] version)))
