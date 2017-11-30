(ns bluegenes.subs.id-resolver
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub ::staged-files
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :files]))))

(reg-sub ::stage-options
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :options]))))

(reg-sub ::stage-status
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :status]))))
