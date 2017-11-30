(ns bluegenes.subs.id-resolver
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub ::staged-files
         (fn [db]
           (get-in db [:idresolver :files])))
