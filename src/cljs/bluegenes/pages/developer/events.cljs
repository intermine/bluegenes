(ns bluegenes.pages.developer.events
  (:require [re-frame.core :refer [reg-event-fx]]
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
                :on-error [::error-tool]}]}))

(reg-event-fx
 ::uninstall-tool
 (fn [_ [_ tool-name]]
   {:dispatch [::tool-operation
               {:method :post
                :uri "/api/tools/uninstall"
                :json-params {:package tool-name}
                :on-success [::success-tool]
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
      ::fx/http request})))

(reg-event-fx
 ::success-tool
 (fn [{db :db} _]
   {:dispatch [::tools/fetch-tools]
    :db (assoc-in db [:tools :npm-working?] false)}))

;; This handler is primarily for the scenario where a different user is
;; performing some tool npm operation, causing the backend to reject this
;; user's request. We won't know when the other user's operation completes, so
;; we'll just have to set `:npm-working?` back to false and alert our user.
(reg-event-fx
 ::error-tool
 (fn [{db :db} _]
   {:dispatch-n [[::tool-operation-busy-message]
                 [::tools/fetch-tools]]
    :db (assoc-in db [:tools :npm-working?] false)}))

(reg-event-fx
 ::tool-operation-busy-message
 (fn [_ _]
   {:dispatch [:messages/add
               {:markup [:span "A tool operation is already in progress. Please try again later."]
                :style "warning"}]}))
