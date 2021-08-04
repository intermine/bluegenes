(ns bluegenes.components.tools.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.effects :as fx]
            [bluegenes.crud.tools :as crud]))

(reg-event-fx
 ::fetch-tools
 (fn [{db :db} [evt]]
   {:db db
    ::fx/http {:method :get
               :uri "/api/tools/all"
               :on-success [::success-fetch-tools]
               :on-unauthorised [::error-fetch-tools evt]
               :on-failure [::error-fetch-tools evt]}}))

(reg-event-db
 ::success-fetch-tools
 (fn [db [_ {:keys [tools]}]]
   (crud/update-installed-tools db tools)))

(reg-event-fx
 ::error-fetch-tools
 (fn [{db :db} [_ evt res]]
   (let [text (case evt
                ::fetch-tools "Failed to fetch BlueGenes Tools - visualizations may not show. This indicates a problem with the BlueGenes backend. "
                "Error occurred when communicating with the BlueGenes Tool backend. ")]
     {:dispatch [:messages/add
                 {:markup [:span text
                           (when-let [err (get-in res [:body :error])]
                             [:code err])]
                  :style "warning"
                  :timeout 0}]})))

(reg-event-fx
 ::fetch-npm-tools
 (fn [_ [evt]]
   {::fx/http {:method :get
               :uri (str "https://api.npms.io/v2/search"
                         "?q=keywords:bluegenes-intermine-tool")
               :on-success [::success-fetch-npm-tools]
               :on-unauthorised [::error-fetch-tools evt]
               :on-failure [::error-fetch-tools evt]}}))

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
 (fn [_ [evt]]
   {::fx/http {:method :get
               :uri "/api/tools/path"
               :on-success [::success-fetch-tool-path]
               :on-unauthorised [::error-fetch-tools evt]
               :on-failure [::error-fetch-tools evt]}}))

(reg-event-db
 ::success-fetch-tool-path
 (fn [db [_ {:keys [path]}]]
   (assoc-in db [:tools :path] path)))

(reg-event-fx
 ::init-tool
 (fn [{db :db} [_ tool-details tool-id]]
   (let [mine     (get-in db [:mines (:current-mine db)])
         service  (get mine :service)]
     {:load-tool {:tool tool-details
                  :tool-id tool-id
                  :service service}})))

(reg-event-db
 ::collapse-tool
 (fn [db [_ tool-name-cljs]]
   (update-in db [:tools :collapsed] (fnil conj #{}) tool-name-cljs)))

(reg-event-db
 ::expand-tool
 (fn [db [_ tool-name-cljs]]
   (update-in db [:tools :collapsed] (fnil disj #{}) tool-name-cljs)))
