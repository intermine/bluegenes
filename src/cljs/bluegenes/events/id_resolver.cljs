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
                {:db (assoc-in db [:idresolver :stage :status] {:action :parsing})
                 ::fx/http {:uri "/api/ids/parse"
                            :method :post
                            :multipart-params (conj
                                                (map (fn [f] [(oget f :name) f]) js-Files)
                                                ["caseSensitive" case-sensitive?])
                            :body {:caseSensitive? true}
                            :on-success [::store-parsed-response]}}))

(reg-event-db ::store-parsed-response
              (fn [db [_ response]]
                (update db :idresolver #(-> %
                                            (assoc-in [:stage :status :action] nil)
                                            (assoc :to-resolve response)))))

(reg-event-db ::toggle-case-sensitive
              (fn [db [_ response]]
                (update-in db [:idresolver :stage :options :case-sensitive?] not)))

(reg-event-db ::update-option
              (fn [db [_ option-kw value]]
                (assoc-in db [:idresolver :stage :options option-kw] value)))