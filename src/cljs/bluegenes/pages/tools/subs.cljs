(ns bluegenes.pages.tools.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
 ::tool-working?
 (fn [db]
   (get-in db [:tools :tool-working?])))
