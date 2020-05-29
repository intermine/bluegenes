(ns bluegenes.components.tools.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.effects :as fx]))

(reg-event-fx
 ::fetch-tools
 (fn [{db :db} [_]]
   {:db db
    ::fx/http {:method :get
               :uri "/api/tools/all"
               :on-success [::success-fetch-tools]}}))

(reg-event-db
 ::success-fetch-tools
 (fn [db [_ {:keys [tools]}]]
   (assoc-in db [:tools :installed] tools)))

(reg-event-fx
 ::fetch-npm-tools
 (fn [_ _]
   {::fx/http {:method :get
               :uri (str "https://api.npms.io/v2/search"
                         "?q=keywords:bluegenes-intermine-tool")
               :on-success [::success-fetch-npm-tools]}}))

(reg-event-db
 ::success-fetch-npm-tools
 (fn [db [_ {:keys [results]}]]
   (assoc-in db [:tools :available] results)))

(reg-event-fx
 ::navigate-query
 (fn [{db :db} [_ query source]]
   (let [source (or source (:current-mine db))
         set-current-mine [:set-current-mine source]
         history+         [:results/history+ {:source source
                                              :type :query
                                              :intent :tool
                                              :value query}]
         new-source?      (not= source (:current-mine db))]
     {:dispatch (if new-source? set-current-mine history+)
      ;; Use :dispatch-after-boot since [:results :queries] is cleared when switching mines.
      :db (cond-> db
            new-source? (update :dispatch-after-boot (fnil conj []) history+))})))

(reg-event-fx
 ::fetch-tool-path
 (fn [_ [_]]
   {::fx/http {:method :get
               :uri "/api/tools/path"
               :on-success [::success-fetch-tool-path]}}))

(reg-event-db
 ::success-fetch-tool-path
 (fn [db [_ {:keys [path]}]]
   (assoc-in db [:tools :path] path)))

(reg-event-fx
 ::load-tools
 (fn [{db :db} [_]]
   (let [tools    (get-in db [:tools :installed])
         service  (get-in db [:mines (:current-mine db) :service])
         entities (get-in db [:tools :entities])]
     (if (empty? tools)
       {}
       {:load-suitable-tools {:tools tools
                              :service service
                              :entities entities}}))))
