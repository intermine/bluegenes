(ns bluegenes.events.id-resolver
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db ::stage-files
              (fn [db [_ js-FileList]]
                (update-in db [:idresolver :files] concat (array-seq js-FileList))))

(reg-event-db ::unstage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :files] #(remove (partial = js-File) %))))