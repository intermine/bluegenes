(ns bluegenes.pages.profile.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::responses
 (fn [db [_ kw]]
   (get-in db [:profile :responses kw])))
