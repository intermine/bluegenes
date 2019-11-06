(ns bluegenes.pages.developer.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
 ::panel
 (fn [db]
   (get-in db [:debug-panel])))

(reg-sub
 ::tool-working?
 (fn [db]
   (get-in db [:tools :tool-working?])))

(reg-sub
 ::tools-path
 (fn [db]
   (get-in db [:tools :path])))
