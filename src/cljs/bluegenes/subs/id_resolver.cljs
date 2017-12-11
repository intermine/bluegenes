(ns bluegenes.subs.id-resolver
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub ::staged-files
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :files]))))

(reg-sub ::textbox-identifiers
         (fn [db]
           (get-in db [:idresolver :stage :textbox])))

(reg-sub ::stage-options
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :options]))))

(reg-sub ::stage-status
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :status]))))

(reg-sub ::stage-flags
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :flags]))))

(reg-sub ::resolution-response
         (fn [db]
           (not-empty (get-in db [:idresolver :response]))))

(reg-sub ::list-name
         (fn [db]
           (get-in db [:idresolver :save :list-name])))

(reg-sub ::resolution-stats
         (fn [db]
           (get-in db [:idresolver :response :stats])))

(reg-sub ::view
         (fn [db]
           (get-in db [:idresolver :stage :view])))