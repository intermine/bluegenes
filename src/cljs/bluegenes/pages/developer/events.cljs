(ns bluegenes.pages.developer.events
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [bluegenes.components.tools.events :as tools]
            [bluegenes.effects :as fx]))

(reg-event-fx
 ::panel
 (fn [{db :db} [_ panel-name]]
   (cond-> {:db (assoc db :debug-panel panel-name)}
     (= panel-name "tool-store")
     (assoc :dispatch-n [[::tools/fetch-tools]
                         [::tools/fetch-npm-tools]]))))

(reg-event-fx
 ::install-tool
 (fn [_ [_ tool-name]]
   {:dispatch [::tool-operation
               {:method :post
                :uri "/api/tools/install"
                :json-params {:package tool-name}
                :on-success [::success-tool]
                :on-unauthorised [::error-tool]
                :on-error [::error-tool]}]}))

(reg-event-fx
 ::uninstall-tool
 (fn [_ [_ tool-name]]
   {:dispatch [::tool-operation
               {:method :post
                :uri "/api/tools/uninstall"
                :json-params {:package tool-name}
                :on-success [::success-tool]
                :on-unauthorised [::error-tool]
                :on-error [::error-tool]}]}))

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
                  :on-error [::error-tool]}]})))

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
                  :on-error [::error-tool]}]})))

;; # How we communicate tool npm operations to the user
;;     SUB   :bluegenes.pages.developer.subs/npm-working?
;; - Indicates that an npm operation is in progress and should complete
;;   shortly, at which point this will be set back to false.
;;     EVENT :bluegenes.pages.developer.events/tool-operation-busy-message
;; - Shows an alert to inform the user that an npm operation is already in
;;   progress and that they should wait and try again later.

;; All tool npm operations should dispatch this event instead of `::fx/http`
;; directly, so we can stop the client from running additional operations while
;; one is already in progress.
(reg-event-fx
 ::tool-operation
 (fn [{db :db} [_ request]]
   (if (get-in db [:tools :npm-working?])
     {:dispatch [::tool-operation-busy-message]}
     {:db (assoc-in db [:tools :npm-working?] true)
      ::fx/http (update request :json-params assoc
                        ;; We don't use the current-mine, as the privilege
                        ;; check only runs on the configured default root.
                        :service (select-keys (get-in db [:mines :default :service])
                                              [:root :token]))})))

(reg-event-db
 ::success-tool
 (fn [db [_ {:keys [tools]}]]
   (update db :tools assoc
           :npm-working? false
           :installed tools)))

;; This is to handle two scenarios:
;; 1. A different user is performing some tool npm operation, causing the
;; backend to reject this user's request.  We won't know when the other user's
;; operation completes, so we'll just have to set `:npm-working?` back to false
;; and alert our user.
;; 2. The current user's request was rejected by the backend as they don't have
;; the privilege to change the tool store. We'll again set `:npm-working?` back
;; to false and alert our user.
(reg-event-fx
 ::error-tool
 (fn [{db :db} [_ res]]
   (let [msg [:messages/add
              {:markup [:span (get-in res [:body :error])]
               :style "warning"
               :timeout 0}]]
     {:dispatch-n [msg [::tools/fetch-tools]]
      :db (assoc-in db [:tools :npm-working?] false)})))

(reg-event-fx
 ::tool-operation-busy-message
 (fn [_ _]
   {:dispatch [:messages/add
               {:markup [:span "A tool operation is already in progress. Please try again later."]
                :style "warning"}]}))
