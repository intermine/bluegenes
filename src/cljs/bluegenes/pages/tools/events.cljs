(ns bluegenes.pages.tools.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :as fx]
            [bluegenes.crud.tools :as tools-crud]
            [bluegenes.config :refer [read-default-ns]]))

(reg-event-fx
 ::init
 (fn [{db :db} [_]]
   {:dispatch-n [[::tools/fetch-tools]
                 [::tools/fetch-npm-tools]]}))

(reg-event-fx
 ::install-tool
 (fn [_ [_ tool-name]]
   {:dispatch [::tool-operation
               {:method :post
                :uri "/api/tools/install"
                :json-params {:package tool-name}
                :on-success [::success-tool]
                :on-unauthorised [::error-tool]
                :on-failure [::error-tool]}]}))

(reg-event-fx
 ::uninstall-tool
 (fn [_ [_ tool-name]]
   {:dispatch [::tool-operation
               {:method :post
                :uri "/api/tools/uninstall"
                :json-params {:package tool-name}
                :on-success [::success-tool]
                :on-unauthorised [::error-tool]
                :on-failure [::error-tool]}]}))

(reg-event-fx
 ::update-all-tools
 (fn [{db :db} _]
   (let [installed-tools (get-in db [:tools :installed])
         tool-names (mapv #(-> % :package :name) installed-tools)]
     {:dispatch [::tool-operation
                 {:method :post
                  :uri "/api/tools/update"
                  :json-params {:packages tool-names}
                  :on-success [::success-tool]
                  :on-unauthorised [::error-tool]
                  :on-failure [::error-tool]}]})))

(reg-event-fx
 ::install-all-tools
 (fn [{db :db} _]
   (let [available-tools (get-in db [:tools :available])
         tool-names (mapv #(-> % :package :name) available-tools)]
     {:dispatch [::tool-operation
                 {:method :post
                  :uri "/api/tools/install"
                  :json-params {:packages tool-names}
                  :on-success [::success-tool]
                  :on-unauthorised [::error-tool]
                  :on-failure [::error-tool]}]})))

;; # How we communicate tool operations to the user
;;     SUB   :bluegenes.pages.developer.subs/tool-working?
;; - Indicates that a tool operation is in progress and should complete
;;   shortly, at which point this will be set back to false.
;;     EVENT :bluegenes.pages.developer.events/tool-operation-busy-message
;; - Shows an alert to inform the user that a tool operation is already in
;;   progress and that they should wait and try again later.

;; All tool operations should dispatch this event instead of `::fx/http`
;; directly, so we can stop the client from running additional operations while
;; one is already in progress.
(reg-event-fx
 ::tool-operation
 (fn [{db :db} [_ request]]
   (if (get-in db [:tools :tool-working?])
     {:dispatch [::tool-operation-busy-message]}
     {:db (assoc-in db [:tools :tool-working?] true)
      ::fx/http (update request :json-params assoc
                        ;; We don't use the current-mine, as the privilege
                        ;; check only runs on the configured default root.
                        :service (select-keys (get-in db [:mines (read-default-ns) :service])
                                              [:root :token])
                        ;; Used to display more informative error messages.
                        :mine-name (get-in db [:mines (read-default-ns) :name]))})))

(reg-event-db
 ::success-tool
 (fn [db [_ {:keys [tools]}]]
   (-> db
       (assoc-in [:tools :tool-working?] false)
       (tools-crud/update-installed-tools tools))))

;; This is to handle two scenarios:
;; 1. A different user is performing some tool operation, causing the backend
;; to reject this user's request.  We won't know when the other user's
;; operation completes, so we'll just have to set `:tool-working?` back to false
;; and alert our user.
;; 2. The current user's request was rejected by the backend as they don't have
;; the privilege to change the tool store. We'll again set `:tool-working?` back
;; to false and alert our user.
(reg-event-fx
 ::error-tool
 (fn [{db :db} [_ res]]
   (let [msg [:messages/add
              {:markup [:span (or (get-in res [:body :error])
                                  "Error occurred in Tool server. Please try again later.")]
               :style "warning"
               :timeout 0}]]
     {:dispatch-n [msg [::tools/fetch-tools]]
      :db (assoc-in db [:tools :tool-working?] false)})))

(reg-event-fx
 ::tool-operation-busy-message
 (fn [_ _]
   {:dispatch [:messages/add
               {:markup [:span "A tool operation is already in progress. Please try again later."]
                :style "warning"}]}))
