(ns bluegenes.events.id-resolver
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db ::stage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :files] conj js-File)))

(reg-event-db ::unstage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :files] #(remove (partial = js-File) %))))