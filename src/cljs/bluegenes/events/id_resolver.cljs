(ns bluegenes.events.id-resolver
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [oops.core :refer [oget]]))

(reg-event-db ::stage-files
              (fn [db [_ js-FileList]]
                (update-in db [:idresolver :stage :files] concat (array-seq js-FileList))))

(reg-event-db ::unstage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :stage :files] #(remove (partial = js-File) %))))

(reg-event-fx ::parse-staged-files
              (fn [{db :db} [_ js-Files case-sensitive?]]
                {::fx/http {:uri "/api/ids/parse"
                            :method :post
                            ;:form-params {:caseSensitive true
                            ;              :files js-Files}
                            :multipart-params (conj
                                                (map (fn [f] [(oget f :name) f]) js-Files)
                                                ["caseSensitive" case-sensitive?])
                            :body {:caseSensitive? true}
                            :on-success [::store-parsed-response]}}))

(reg-event-db ::store-parsed-response
              (fn [db [_ response]]
                (js/console.log "response" response)
                db))

(reg-event-db ::toggle-case-sensitive
              (fn [db [_ response]]
                (update-in db [:idresolver :stage :options :case-sensitive?] not)))