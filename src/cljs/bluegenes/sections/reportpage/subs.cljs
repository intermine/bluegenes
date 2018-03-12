(ns bluegenes.sections.reportpage.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub ::a-table
         (fn [db [_ location]]
           (get-in db location)))