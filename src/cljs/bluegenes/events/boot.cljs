(ns bluegenes.events.boot
  (:require [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx]]
            [bluegenes.db :as db]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [bluegenes.route :as route]
            [bluegenes.utils :as utils]
            [bluegenes.pages.lists.events :as lists]
            [bluegenes.config :refer [server-vars init-vars read-default-ns]]))

(defn boot-flow
  "Produces a set of re-frame instructions that load all of InterMine's assets into BlueGenes
  See https://github.com/Day8/re-frame-async-flow-fx
  The idea is that any URL routing (such as entering BlueGenes at the home page or a subsection)
  is queued until all of the assets (data model, lists, templates etc) are fetched.
  When finished, an event called :finished-loading-assets is dispatch which tells BlueGenes
  it can continue routing."
  [& {:keys [wait-registry?]}]
  ;; wait-registry? is to indicate that the current mine requries data from the
  ;; registry, which isn't present (usually because it is a fresh boot).
  {:id :async/custom-flow
   :db-path [:boot-flow]
   :rules (filterv
           some?
           [(when wait-registry?
              {:when :seen?
               :events :bluegenes.events.registry/success-fetch-registry
               :dispatch [:authentication/init]})
            ;; Wait for token as some assets need it for private data (eg. lists, queries).
            {:when :seen?
             :events :authentication/store-token
             :dispatch-n [[:assets/fetch-web-properties]
                          [:assets/fetch-bg-properties]
                          [:assets/fetch-model]
                          [:assets/fetch-lists]
                          [:assets/fetch-class-keys]
                          [:assets/fetch-templates]
                          [:assets/fetch-widgets]
                          [:assets/fetch-summary-fields]
                          [:assets/fetch-intermine-version]
                          [:assets/fetch-web-service-version]
                          [:assets/fetch-release-version]
                          [:assets/fetch-branding]
                          ;; Errors for tool fetching are handled separately.
                          [:bluegenes.components.tools.events/fetch-tools]]}
            ;; Wait for all events that indicate our assets have been fetched successfully.
            {:when :seen-all-of?
             :events [:assets/success-fetch-model
                      :assets/success-fetch-web-properties
                      :assets/success-fetch-bg-properties
                      :assets/success-fetch-lists
                      :assets/success-fetch-class-keys
                      :assets/success-fetch-templates
                      :assets/success-fetch-summary-fields
                      :assets/success-fetch-widgets
                      :assets/success-fetch-intermine-version
                      :assets/success-fetch-web-service-version
                      :assets/success-fetch-release-version
                      :assets/success-fetch-branding]
             :dispatch [:boot/finalize]
             :halt? true}
            ;; Handle the case where one or more assets fail to fetch.
            {:when :seen?
             :events :assets/failure
             :dispatch [:boot/finalize :failed-assets? true]
             :halt? true}])})

;; It's not really optimal that `:assets/failure` starts `:boot/finalize`
;; immediately. This may also (through a race condition) log errors that the
;; `:async/custom-flow` event handler is missing, since it would have been
;; deregistered. It's very unlikely that this will actually break something,
;; and we shouldn't optimize for faulty webservices. Just to note, the proper
;; way would be to wait for all `:assets/*` to either succeed or fail, then
;; halt and run `:boot/finalize`. Right now any failure will result in
;; `:boot/finalize` running, even with other `:assets/*` still in progress.

(reg-event-fx
 :boot/finalize
 (fn [_ [_ & {:keys [failed-assets?]}]]
   {:dispatch-n
    [;; Verify InterMine web service version.
     [:verify-web-service-version]
     ;; Start Google Analytics.
     [:start-analytics]
     ;; Set a flag indicating all assets are fetched.
     [:finished-loading-assets]
     ;; Save the current state to local storage.
     (when-not failed-assets? [:save-state])
     ;; fetch-organisms doesn't always load before it is needed.
     ;; for example on a fresh load of the id resolver, I sometimes end up with
     ;; no organisms when I initialise the component. I have a workaround
     ;; so it doesn't matter in this case, but it is something to be aware of.
     [:cache/fetch-organisms]
     [:regions/select-all-feature-types]]}))

(defn im-tables-events-forwarder
  "Creates instructions for listening in on im-tables events.
  Why? im-tables is its own re-frame application and it can save query results.
  When its save-list-success event is seen, fire a BlueGenes event to re-fetch lists"
  []
  {:register :im-tables-events
   :events #{:imt.io/save-list-success :imt.io/save-list-failure}
   :dispatch-to [:intercept-save-list]})

; When a list is saved from im-tables, intercept the message
; and show an alert while also refreshing the user's lists
(reg-event-fx
 :intercept-save-list
 (fn [{db :db} [_ [evt-id {:keys [listName listSize] :as evt}]]]
   (case evt-id
     :imt.io/save-list-success
     {:dispatch-n [[:assets/fetch-lists]
                   [:messages/add
                    {:markup [:span "Saved list: "
                              [:a {:href (route/href ::route/results {:title listName})}
                               listName]]
                     :style "success"}]]}

     :imt.io/save-list-failure
     {:dispatch [:messages/add
                 {:markup [:span (str "Failed to save list: " listName)]
                  :style "danger"}]}

     (.error js/console (str "Unhandled forwarded im-tables event " evt-id)))))

(defn init-configured-mine
  [{:keys [root name namespace]}]
  {:id (keyword namespace)
   :name name
   :service {:root root}})

(defn init-mine-defaults
  "If this bluegenes instance is coupled with InterMine, load the intermine's
  config directly from env variables passed to bluegenes. Otherwise, create a
  default mine config.
  You can specify `:token my-token` if you want to reuse an existing token."
  [& {:keys [token]}]
  {:id (read-default-ns)
   :name (:bluegenes-default-mine-name @server-vars)
   :service {:root (:bluegenes-default-service-root @server-vars)
             :token token}})

(defn wait-for-registry?
  "If the URL corresponds to a non-configured mine, we will have to fetch the
  registry and look for the mine with the same namespace, (user will be
  redirected to the default mine if it doesn't exist) before we attempt
  authentication. Returns whether this is the case."
  [db config-ns-set]
  (let [current-mine (:current-mine db)
        configured? (contains? config-ns-set current-mine)
        ;; Registry will only be empty for the initial boot, not for reboots.
        in-registry? (contains? (:registry db) current-mine)]
    (if (:hide-registry-mines @server-vars)
      false
      (not (or configured? in-registry?)))))

(defn init-config-mines []
  (->> (:bluegenes-additional-mines @server-vars)
       (map init-configured-mine)
       (into [(init-mine-defaults)])
       (reduce (fn [mines {:keys [id] :as mine}]
                 (assoc mines id mine))
               {})))

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
         init-events (some-> @init-vars :events not-empty)
         configured-mines (init-config-mines)
         all-config-ns (keys configured-mines)
         default-ns (read-default-ns)
         init-db
         (-> db/default-db
             (assoc-in [:env :mines] configured-mines)
             ;; Add default mine, either as is configured when attached to an
             ;; InterMine instance, or as an empty placeholder.
             (assoc-in [:mines default-ns] (init-mine-defaults))
             (assoc :current-mine (or selected-mine default-ns))
             (cond-> init-events (assoc :dispatch-after-boot init-events)))
         ;; Get data previously persisted to local storage.
         {:keys [mines assets version] :as state} (:local-store cofx)
         ;; The token for the default mine is a special case. The persisted map
         ;; for the default will always be overwritten, so we pass it to
         ;; init-mine-defaults here to put it back in there.
         persisted-default-token (get-in state [:mines default-ns :service :token])
         ;; We always want `init-mine-defaults` to override the default mine
         ;; saved in local storage, as a coupled intermine instance should
         ;; always take priority.
         updated-mines (assoc mines default-ns (init-mine-defaults :token persisted-default-token))
         db (cond-> init-db
              ;; Only use data from local storage if it's non-empty and the
              ;; client version matches.
              (and (seq state)
                   (= version (:version @server-vars)))
              (assoc :mines updated-mines
                     :assets assets
                     ;; This needs to be true so we can block `:set-active-panel`
                     ;; event until we have `:finished-loading-assets`, as some
                     ;; routes might attempt to build a query which is dependent
                     ;; on `db :assets :summary-fields` before it gets populated
                     ;; by `:assets/success-fetch-summary-fields`.
                     :fetching-assets? true))
         wait-registry? (wait-for-registry? db (set all-config-ns))]
     {:db db
      :dispatch-n (if wait-registry?
                    ;; Wait with authentication until registry is loaded.
                    [[:bluegenes.events.registry/load-other-mines]]
                    ;; Do both at the same time!
                    [[:authentication/init]
                     [:bluegenes.events.registry/load-other-mines]])
      ;; Start the timer for showing the loading dialog.
      :mine-loader true
      ;; Boot the application asynchronously
      :async-flow (boot-flow :wait-registry? wait-registry?)
      ;; Register an event sniffer for im-tables
      :forward-events (im-tables-events-forwarder)})))

(defn remove-stateful-keys-from-db
  "Any tools / components that have mine-specific state should lose that
   state if we switch mines. For example, in list upload (ID Resolver),
   drosophila IDs are no longer valid when using humanmine."
  [db]
  ;; Perhaps we should consider settings `:assets` to `{}` here as well?
  (-> db
      ;; Other mines might have a different model, so we reset the querybuilder.
      (assoc :qb (:qb db/default-db))
      ;; The below were undocumented; please add a comment for each of them
      ;; when you learn why they're there.
      (dissoc :regions :idresolver :results
              :suggestion-results ; Avoid showing old results belonging to previous mine.
              :invalid-token?     ; Clear invalid-token-alert.
              :failed-auth?)      ; Clear flag for failing to auth with mine.
      ;; Clear chosen template category as it may not be present in new mine.
      (update :home dissoc :active-template-category)
      ;; Set lists page number back to 1.
      (assoc-in [:lists :pagination :current-page] 1)
      ;; Clear lists page selected lists.
      (update-in [:lists :selected-lists] empty)
      ;; The old by-id map is used to detect newly added lists.
      ;; During a mine change, this will mean all lists, which we don't want.
      (update-in [:lists :by-id] empty)))

(reg-event-fx
 :reboot
 (fn [{db :db}]
   {:db (assoc (remove-stateful-keys-from-db db) :fetching-assets? true)
    :dispatch [:authentication/init]
    :async-flow (boot-flow :wait-registry? false)}))

;; Note that failed-auth? can mean there's no connection to the mine.
(reg-event-fx
 :finished-loading-assets
 (fn [{db :db}]
   (let [dispatch-after-boot (:dispatch-after-boot db)
         ;; We want to block navigation on failed auth, as this could cause a
         ;; crash when opening a page that expects a mine to be connected.
         failed-auth? (:failed-auth? db)
         ;; If we're going to change panel, we'll wait with :hide-intro-loader
         ;; until it's done (otherwise there will be a flash of the home page).
         will-change-panel? (contains? (set (map first dispatch-after-boot))
                                       :do-active-panel)]
     (cond-> {:db (-> db
                      (dissoc :dispatch-after-boot)
                      (assoc :fetching-assets? false))
              :mine-loader false}
       (and (some? dispatch-after-boot)
            (not failed-auth?)) (assoc :dispatch-n dispatch-after-boot)
       (or (not will-change-panel?)
           ;; Even if we're supposed to show a different panel, we'll show the
           ;; homepage instead on failed auth (navigation will be blocked).
           failed-auth?) (assoc :hide-intro-loader nil)))))

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
   (let [analytics-id (:google-analytics @server-vars)
         analytics-enabled? (not (clojure.string/blank? analytics-id))]
     (if analytics-enabled?
        ;;set tracker up if we have a tracking id
       (do
         (try
           (js/ga "create" analytics-id "auto")
           (js/ga "send" "pageview")
           (catch js/Error _))
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
 (fn [{db :db, login :local-store} [evt]]
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
                  :on-unauthorised [:authentication/invalid-token (boolean auth-token)]
                  :on-failure [:authentication/invalid-token (boolean auth-token)]}
                 {:chan (fetch/session service)
                  :on-success [:authentication/store-token]
                  :on-failure [:assets/failure evt]})})))

;; The token has likely expired, so we fetch a new one.
;; We also clear the login token if it had expired.
(reg-event-fx
 :authentication/invalid-token
 (fn [{db :db} [evt clear-login?]]
   (let [current-mine (:current-mine db)]
     (cond-> {:im-chan {:chan (fetch/session (get-in db [:mines current-mine :service]))
                        :on-success [:authentication/store-token]
                        :on-failure [:assets/failure evt]}}
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
       (assoc :dispatch [:messages/add
                         {:markup [:span "Failed to acquire token. It's likely that you have no connection to the InterMine instance."]
                          :style "danger"
                          :timeout 0}])))))

; Fetch model
(def preferred-tag "im:preferredBagType")
(defn preferred-fields
  "extricate preferred fields (e.g. default field types for dropdowns, usually protein and gene) from the model"
  [model]
  (keys (filter (comp #(contains? % preferred-tag)
                      set :tags second) (:classes model))))

(defn extends-hierarchy
  "Subclasses in the model are specified via the extends key. We build a
  first-class hierarchy type using this so we can query whether a class
  is a subclass for another class."
  [model-classes]
  (reduce (fn [h [child {:keys [extends]}]]
            (reduce #(derive %1 child %2) h (map keyword extends)))
          (make-hierarchy)
          model-classes))

(reg-event-db
 :assets/success-fetch-model
 (fn [db [_ mine-kw model]]
   ;; We used to remove empty classes (zero count) from the model here, but
   ;; this turned out to be a very bad idea! This is because the model is used
   ;; to parse paths into classes, which means it has to be complete. This also
   ;; applies to the model hierarchy.
   (-> db
       (assoc-in [:mines mine-kw :service :model] model)
       (assoc-in [:mines mine-kw :default-object-types] (sort (preferred-fields model)))
       (assoc-in [:mines mine-kw :model-hier] (extends-hierarchy (:classes model))))))

(reg-event-fx
 :assets/fetch-model
 (fn [{db :db} [evt]]
   {:db db
    :im-chan {:chan (fetch/model (get-in db [:mines (:current-mine db) :service]))
              :on-success [:assets/success-fetch-model (:current-mine db)]
              :on-failure [:assets/failure evt]}}))

; Fetch lists

(reg-event-fx
 :assets/success-fetch-lists
 (fn [{db :db} [_ mine-kw lists]]
   (merge
    {:db (assoc-in db [:assets :lists mine-kw] lists)}
    ;; Denormalize lists right-away if you're on the lists page.
    (when (= :lists-panel (:active-panel db))
      {:dispatch [:lists/initialize]}))))

;; This event is also dispatched externally from bluegenes.pages.lists.events.
(reg-event-fx
 :assets/fetch-lists
 (fn [{db :db} [evt]]
   {:db (assoc-in db (concat lists/root [:fetching-lists?]) true)
    :im-chan {:chan (fetch/lists
                     (get-in db [:mines (:current-mine db) :service])
                     {:showTags true})
              :on-success [:assets/success-fetch-lists (:current-mine db)]
              :on-failure [:assets/failure evt]}}))

; Fetch class keys

(reg-event-db
 :assets/success-fetch-class-keys
 (fn [db [_ mine-kw class-keys]]
   (assoc-in db [:mines mine-kw :class-keys] class-keys)))

(reg-event-fx
 :assets/fetch-class-keys
 (fn [{db :db} [evt]]
   {:db db
    :im-chan
    {:chan (fetch/class-keys
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-class-keys (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

; Fetch templates

(reg-event-db
 :assets/success-fetch-templates
 (fn [db [_ mine-kw lists]]
   (assoc-in db [:assets :templates mine-kw] lists)))

(reg-event-fx
 :assets/fetch-templates
 (fn [{db :db} [evt]]
   {:db db
    :im-chan
    {:chan (fetch/templates (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-templates (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

; Fetch summary fields

(reg-event-db
 :assets/success-fetch-summary-fields
 (fn [db [_ mine-kw lists]]
   (assoc-in db [:assets :summary-fields mine-kw] lists)))

(reg-event-fx
 :assets/fetch-summary-fields
 (fn [{db :db} [evt]]
   {:db db
    :im-chan
    {:chan (fetch/summary-fields
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-summary-fields (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

(reg-event-fx
 :assets/fetch-widgets
  ;;fetches all enrichment widgets. afaik the non-enrichment widgets are InterMine 1.x UI specific so are filtered out upon success
 (fn [{db :db} [evt]]
   {:im-chan
    {:chan (fetch/widgets (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-widgets (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

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
 (fn [{db :db} [evt]]
   {:im-chan
    {:chan (fetch/version-intermine
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-intermine-version (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

(reg-event-db
 :assets/success-fetch-intermine-version
 (fn [db [_ mine-kw version]]
   (assoc-in db [:assets :intermine-version mine-kw] version)))

(reg-event-fx
 :assets/fetch-web-service-version
 (fn [{db :db} [evt]]
   {:im-chan
    {:chan (fetch/version-web-service
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-web-service-version (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

(reg-event-db
 :assets/success-fetch-web-service-version
 (fn [db [_ mine-kw version]]
   (assoc-in db [:assets :web-service-version mine-kw] version)))

(reg-event-fx
 :assets/fetch-release-version
 (fn [{db :db} [evt]]
   {:im-chan
    {:chan (fetch/version-release
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-release-version (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

(reg-event-db
 :assets/success-fetch-release-version
 (fn [db [_ mine-kw version]]
   (assoc-in db [:assets :release-version mine-kw] version)))

(reg-event-fx
 :assets/fetch-branding
 (fn [{db :db} [evt]]
   {:im-chan
    {:chan (fetch/branding
            (get-in db [:mines (:current-mine db) :service]))
     :on-success [:assets/success-fetch-branding (:current-mine db)]
     :on-failure [:assets/failure evt]}}))

(reg-event-db
 :assets/success-fetch-branding
 (fn [db [_ mine-kw branding-properties]]
   (assoc-in db [:mines mine-kw :branding] branding-properties)))

(reg-event-fx
 :assets/failure
 (fn [{db :db} [_ evt]]
   (let [mine-name (get-in db [:mines (:current-mine db) :name])]
     {:db (case evt
            (:authentication/init :authentication/invalid-token)
            (assoc db :failed-auth? true)
            db)
      :dispatch (case evt
                  (:authentication/init :authentication/invalid-token)
                  [:messages/add
                   {:markup [:span "Failed to acquire token. It's likely that you have no connection to the InterMine instance."]
                    :style "danger"
                    :timeout 0}]

                  [:messages/add
                   {:markup [:span "Failed " [:em (utils/kw->str evt)] " from " [:em mine-name] ". "
                             "Please contact the maintainers of the InterMine instance. BlueGenes may work in reduced capacity."]
                    :style "warning"
                    :timeout 0}])})))
