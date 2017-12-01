(ns bluegenes.events.id-resolver
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [oops.core :refer [oget]]
            [imcljs.fetch :as fetch]))

(reg-event-db ::stage-files
              (fn [db [_ js-FileList]]
                (update-in db [:idresolver :stage :files] concat (array-seq js-FileList))))

(reg-event-db ::unstage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :stage :files] #(remove (partial = js-File) %))))

(reg-event-fx ::parse-staged-files
              (fn [{db :db} [_ js-Files options]]
                (println "OPT" options)
                {:db (assoc-in db [:idresolver :stage :status] {:action :parsing})
                 ::fx/http {:uri "/api/ids/parse"
                            :method :post
                            :multipart-params (conj
                                                (map (fn [f] [(oget f :name) f]) js-Files)
                                                ["caseSensitive" (:case-sensitive options)])
                            :body {:caseSensitive? true}
                            :on-success [::store-parsed-response options]}}))

(reg-event-fx ::store-parsed-response
              (fn [{db :db} [_ options response]]
                {:db (update db :idresolver #(-> %
                                                 (assoc-in [:stage :status :action] nil)
                                                 (assoc-in [:stage :flags :parsed] true)
                                                 (assoc :to-resolve response)))
                 :dispatch [::resolve-identifiers options (:identifiers response)]}))

(reg-event-fx ::resolve-identifiers
              (fn [{db :db} [_ options identifiers]]
                {:im-chan {:chan (fetch/resolve-identifiers
                                   ;TODO - Just a placeholder, make this dynamic
                                   {:root "beta.flymine.org/beta"}
                                   {:identifiers identifiers
                                    :case-sensitive (:case-sensitive options)
                                    :type "Gene"})
                           :on-success [::store-identifiers]}}))

(reg-event-db ::store-identifiers
              (fn [db [_ response]]
                (assoc-in db [:idresolver :response] response)))

(reg-event-db ::toggle-case-sensitive
              (fn [db [_ response]]
                (update-in db [:idresolver :stage :options :case-sensitive?] not)))

(reg-event-db ::update-option
              (fn [db [_ option-kw value]]
                (assoc-in db [:idresolver :stage :options option-kw] value)))