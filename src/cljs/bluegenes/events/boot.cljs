(ns bluegenes.events.boot
  (:require [re-frame.core :refer [reg-event-db reg-event-fx subscribe inject-cofx]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [bluegenes.events.webproperties]
            [bluegenes.events.registry :as registry]
            [bluegenes.route :as route]
            [cljs-bean.core :refer [->clj]]))

(defn boot-flow
  "Produces a set of re-frame instructions that load all of InterMine's assets into BlueGenes
  See https://github.com/Day8/re-frame-async-flow-fx
  The idea is that any URL routing (such as entering BlueGenes at the home page or a subsection)
  is queued until all of the assets (data model, lists, templates etc) are fetched.
  When finished, an event called :finished-loading-assets is dispatch which tells BlueGenes
  it can continue routing."
  [wait-registry?]
  ;; wait-registry? is to indicate that the current mine requries data from the
  ;; registry, which isn't present (usually because it is a fresh boot).
  {:first-dispatch (if wait-registry?
                     [::registry/load-other-mines]
                     [:authentication/init])
   :rules (filterv
           some?
           [(when wait-registry?
              {:when :seen?
               :events ::registry/success-fetch-registry
               :dispatch [:authentication/init]})
            ;; Wait for token as some assets need it for private data (eg. lists, queries).
            {:when :seen?
             :events :authentication/store-token
             :dispatch-n [[:assets/fetch-web-properties]
                          [:assets/fetch-model]
                          [:assets/fetch-lists]
                          [:assets/fetch-class-keys]
                          [:assets/fetch-templates]
                          [:assets/fetch-widgets]
                          [:assets/fetch-summary-fields]
                          [:assets/fetch-intermine-version]
                          [:assets/fetch-web-service-version]]}
            ;; Wait for all events that indicate our assets have been fetched successfully.
            {:when :seen-all-of?
             :events [:assets/success-fetch-model
                      :assets/success-fetch-web-properties
                      :assets/success-fetch-lists
                      :assets/success-fetch-class-keys
                      :assets/success-fetch-templates
                      :assets/success-fetch-summary-fields
                      :assets/success-fetch-widgets
                      :assets/success-fetch-intermine-version
                      :assets/success-fetch-web-service-version]
             ;; Now finish setting up BlueGenes!
             :dispatch-n
             (cond-> [;; Verify InterMine web service version.
                      [:verify-web-service-version]
                      ;; Start Google Analytics.
                      [:start-analytics]
                      ;; Set a flag indicating all assets are fetched.
                      [:finished-loading-assets]
                      ;; Save the current state to local storage.
                      [:save-state]
                      ;; fetch-organisms doesn't always load before it is needed.
                      ;; for example on a fresh load of the id resolver, I sometimes end up with
                      ;; no organisms when I initialise the component. I have a workaround
                      ;; so it doesn't matter in this case, but it is something to be aware of.
                      [:cache/fetch-organisms]
                      [:regions/select-all-feature-types]]
               (not wait-registry?) (conj [::registry/load-other-mines]))
             :halt? true}])})

(defn im-tables-events-forwarder
  "Creates instructions for listening in on im-tables events.
  Why? im-tables is its own re-frame application and it can save query results.
  When its save-list-success event is seen, fire a BlueGenes event to re-fetch lists"
  []
  {:register :im-tables-events ;;  <-- used
   :events #{:imt.io/save-list-success}
   :dispatch-to [:intercept-save-list]})

; When a list is saved from im-tables, intercept the message
; and show an alert while also refreshing the user's lists
(reg-event-fx
 :intercept-save-list
 (fn [{db :db} [_ [_ {:keys [listName listSize] :as evt}]]]
   {:db (update db :messages conj)
    :dispatch-n
    [[:assets/fetch-lists]
     [:messages/add
      {:markup [:span (str "Saved list to My Data: " listName)]
       :style "success"}]]}))

(defn init-mine-defaults
  "If this bluegenes instance is coupled with InterMine, load the intermine's
  config directly from env variables passed to bluegenes. Otherwise, create a
  default mine config.
  You can specify `:token my-token` if you want to reuse an existing token."
  [& {:keys [token]}]
  (let [{:keys [serviceRoot mineName]} (->clj js/serverVars)]
    (if serviceRoot
      {:id :default
       :name mineName
       :service {:root serviceRoot
                 :token token}}
      {:id :default
       :name nil
       :service {:root "https://www.flymine.org/flymine"
                 :token token}})))

(defn wait-for-registry?
  [db]
  (let [current-mine (:current-mine db)
        default? (= current-mine :default)
        ;; There might exist a scenario where it would be better to check for
        ;; the mine to be present in :mines instead of the registry.
        in-registry? (contains? (:registry db) current-mine)]
    (not (or default? in-registry?))))

;; Boot the application.
(reg-event-fx
 :boot
 [(inject-cofx :local-store :bluegenes/state)]
 (fn [cofx _]
   (let [;; We have to set the db current-mine using `window.location` as the
         ;; router won't have dispatched `:set-current-mine` before later on.
         selected-mine (-> (.. js/window -location -pathname)
                           (clojure.string/split #"/")
                           second
                           keyword)
         init-db
         (-> db/default-db
             ;; Add default mine, either as is configured when attached to an
             ;; InterMine instance, or as an empty placeholder.
             (assoc-in [:mines :default] (init-mine-defaults))
             ;; Note that the final value of current-mine will have the
             ;; following order of priority: pathname > localstorage > :default
             (assoc :current-mine (or selected-mine :default)))
         ;; Get data previously persisted to local storage.
         {:keys [current-mine mines assets version] :as state} (:local-store cofx)
         ;; We always want `init-mine-defaults` to override the :default mine
         ;; saved in local storage, as a coupled intermine instance should
         ;; always take priority.
         persisted-default-token (get-in state [:mines :default :service :token])
         ;; The token for :default mine is a special case. The persisted map
         ;; for :default will always be overwritten, so we pass it to
         ;; init-mine-defaults here to put it back in there.
         updated-mines (assoc mines :default (init-mine-defaults :token persisted-default-token))
         db (cond-> init-db
              ;; Only use data from local storage if it's non-empty and the
              ;; client version matches.
              (and (seq state)
                   (= bluegenes.core/version version))
              (assoc :current-mine (or selected-mine current-mine)
                     :mines updated-mines
                     :assets assets
                     ;; This needs to be true so we can block `:set-active-panel`
                     ;; event until we have `:finished-loading-assets`, as some
                     ;; routes might attempt to build a query which is dependent
                     ;; on `db :assets :summary-fields` before it gets populated
                     ;; by `:assets/success-fetch-summary-fields`.
                     :fetching-assets? true))]
     {:db db
      ;; Boot the application asynchronously
      :async-flow (boot-flow (wait-for-registry? db))
      ;; Register an event sniffer for im-tables
      :forward-events (im-tables-events-forwarder)})))

(defn remove-stateful-keys-from-db
  "Any tools / components that have mine-specific state should lose that
   state if we switch mines. For example, in list upload (ID Resolver),
   drosophila IDs are no longer valid when using humanmine."
  [db]
  ;; Perhaps we should consider settings `:assets` to `{}` here as well?
  (dissoc db :regions :idresolver :results :qb
          :suggestion-results ; Avoid showing old results belonging to previous mine.
          :invalid-token?))  ; Clear invalid-token-alert.

(reg-event-fx
 :reboot
 (fn [{db :db}]
   {:db (assoc (remove-stateful-keys-from-db db) :fetching-assets? true)
    :async-flow (boot-flow (wait-for-registry? db))}))

(reg-event-fx
 :finished-loading-assets
 (fn [{db :db}]
   (let [dispatch-after-boot (:dispatch-after-boot db)]
     (cond-> {:db (-> db
                      (dissoc :dispatch-after-boot)
                      (assoc :fetching-assets? false))}
       (some? dispatch-after-boot) (assoc :dispatch-n dispatch-after-boot)))))

(reg-event-fx
 :verify-web-service-version
 (fn [{db :db}]
   (let [mine (get db :current-mine)
         version (js/Number (get-in db [:assets :web-service-version mine]))]
     (when (and (< version 26)
                (not (zero? version)))
                 ;; In case the web-service-version is an empty string
       (js/alert
        (str "You are using an outdated InterMine WebService version: "
             version
             ". Unexpected behaviour may occur. We recommend updating to version 26 or above."))))
   {:db db}))

(reg-event-fx
 :start-analytics
 (fn [{db :db}]
   (let [analytics-id (:googleAnalytics (->clj js/serverVars))
         analytics-enabled? (not (clojure.string/blank? analytics-id))]
     (if analytics-enabled?
        ;;set tracker up if we have a tracking id
       (do
         (js/ga "create" analytics-id "auto")
         (js/ga "send" "pageview")
         (.info js/console
                "Google Analytics enabled. Tracking ID:"
                analytics-id))
        ;;inobtrusive console message if there's no id
       (.info js/console "Google Analytics disabled. No tracking ID."))
     {:db (assoc db :google-analytics
                 {:enabled? analytics-enabled?
                  :analytics-id analytics-id})})))

;; Figure out how we're going to initialise authentication.
;; There are 3 different cases we need to handle:
;; - The user has logged in previously => Re-use their identity!
;; - The user has a previously persisted anonymous token => Re-use that token!
;; - The user has no persisted token => Get a new one!
;; In the first 2 cases, we also have to make sure the token is still valid, so
;; we use the who-am-i? service for this.
(reg-event-fx
 :authentication/init
 [(inject-cofx :local-store :bluegenes/login)]
 (fn [{db :db, login :local-store} _]
   (let [current-mine (:current-mine db)
         ;; Add any persisted login identities to their respective mines.
         db+logins  (reduce (fn [new-db [mine-kw identity]]
                              (assoc-in new-db [:mines mine-kw :auth :identity] identity))
                            db login)
         auth-token (get-in db+logins [:mines current-mine :auth :identity :token])
         service    (get-in db+logins [:mines current-mine :service])
         anon-token (:token service)
         token      (or auth-token anon-token)]
     {:db db+logins
      :im-chan (if token
                 {:chan (auth/who-am-i? service token)
                  :on-success [:authentication/store-token token]
                  :on-failure [:authentication/invalid-token (boolean auth-token)]}
                 {:chan (fetch/session service)
                  :on-success [:authentication/store-token]})})))

;; The token has likely expired, so we fetch a new one.
;; We also clear the login token if it had expired.
(reg-event-fx
 :authentication/invalid-token
 (fn [{db :db} [_ clear-login?]]
   (let [current-mine (:current-mine db)]
     (cond-> {:im-chan {:chan (fetch/session (get-in db [:mines current-mine :service]))
                        :on-success [:authentication/store-token]}}
       clear-login? (->
                     (assoc :db (update-in db [:mines current-mine] dissoc :auth))
                     (assoc :dispatch [:remove-login current-mine]))))))

;; Store an authentication token for a given mine.
(reg-event-fx
 :authentication/store-token
 (fn [{db :db} [_ token]]
   (let [current-mine (:current-mine db)]
     (cond-> {:db (assoc-in db [:mines current-mine :service :token] token)}
       (nil? token)
       (assoc :dispatch
              (let [service-root (get-in db [:mines current-mine :service :root])]
                [:messages/add
                 {:markup [:span (str "Failed to acquire token. It's likely that you have no connection to the InterMine instance at \"" service-root "\".")]
                  :style "danger"
                  :timeout 0}]))))))

; Fetch model
(def preferred-tag "im:preferredBagType")
(defn preferred-fields
  "extricate preferred fields (e.g. default field types for dropdowns, usually protein and gene) from the model"
  [model]
  (keys (filter (comp #(contains? % preferred-tag)
                      set :tags second) (:classes model))))

(reg-event-db
 :assets/success-fetch-model
 (fn [db [_ mine-kw model]]
   (-> db
       (assoc-in [:mines mine-kw :service :model] model)
       (assoc-in [:mines mine-kw :default-object-types]
                 (sort (preferred-fields model))))))

(reg-event-fx
 :assets/fetch-model
 (fn [{db :db}]
   {:db db
    :im-chan {:chan (fetch/model (get-in db [:mines (:current-mine db) :service]))
              :on-success [:assets/success-fetch-model (:current-mine db)]}}))

; Fetch lists

(reg-event-db
 :assets/success-fetch-lists
 (fn [db [_ mine-kw lists]]
   (assoc-in db [:assets :lists mine-kw] lists)))

(reg-event-fx
 :assets/fetch-lists
 (fn [{db :db}]
   {:db db
    :im-chan
    {:chan (fetch/lists
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-lists (:current-mine db)]}}))

; Fetch class keys

(reg-event-db
 :assets/success-fetch-class-keys
 (fn [db [_ mine-kw class-keys]]
   (assoc-in db [:mines mine-kw :class-keys] class-keys)))

(reg-event-fx
 :assets/fetch-class-keys
 (fn [{db :db}]
   {:db db
    :im-chan
    {:chan (fetch/class-keys
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-class-keys (:current-mine db)]}}))

; Fetch templates

(reg-event-db
 :assets/success-fetch-templates
 (fn [db [_ mine-kw lists]]
   (assoc-in db [:assets :templates mine-kw] lists)))

(reg-event-fx
 :assets/fetch-templates
 (fn [{db :db}]
   {:db db
    :im-chan
    {:chan (fetch/templates (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-templates (:current-mine db)]}}))

; Fetch summary fields

(reg-event-db
 :assets/success-fetch-summary-fields
 (fn [db [_ mine-kw lists]]
   (assoc-in db [:assets :summary-fields mine-kw] lists)))

(reg-event-fx
 :assets/fetch-summary-fields
 (fn [{db :db}]
   {:db db
    :im-chan
    {:chan (fetch/summary-fields
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-summary-fields (:current-mine db)]}}))

(reg-event-fx
 :assets/fetch-widgets
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets are InterMine 1.x UI specific so are filtered out upon success
 (fn [{db :db}]
   {:im-chan
    {:chan (fetch/widgets (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-widgets (:current-mine db)]}}))

(reg-event-db
 :assets/success-fetch-widgets
 (fn [db [_ mine-kw widgets]]
   (let [widget-type "enrichment"
         filtered-widgets
         (doall (filter (fn [widget]
                          (= widget-type (:widgetType widget))) widgets))]
     (assoc-in db [:assets :widgets mine-kw] filtered-widgets))))

(reg-event-fx
 :assets/fetch-intermine-version
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets
  ;;are InterMine 1.x UI specific so are filtered out upon success
 (fn [{db :db}]
   {:im-chan
    {:chan (fetch/version-intermine
            (get-in db [:mines (:current-mine db) :service]))
     :on-success
     [:assets/success-fetch-intermine-version (:current-mine db)]}}))

(reg-event-db
 :assets/success-fetch-intermine-version
 (fn [db [_ mine-kw version]]
   (assoc-in db [:assets :intermine-version mine-kw] version)))

(reg-event-fx
 :assets/fetch-web-service-version
 (fn [{db :db}]
   {:im-chan
    {:chan (fetch/version-web-service
            (get-in db [:mines (:current-mine db) :service]))
     :on-success
     [:assets/success-fetch-web-service-version (:current-mine db)]}}))

(reg-event-db
 :assets/success-fetch-web-service-version
 (fn [db [_ mine-kw version]]
   (assoc-in db [:assets :web-service-version mine-kw] version)))
