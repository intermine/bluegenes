(ns bluegenes.pages.profile.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::requests
 (fn [db [_ kw]]
   (get-in db [:profile :requests kw])))

(reg-sub
 ::responses
 (fn [db [_ kw]]
   (get-in db [:profile :responses kw])))

(reg-sub
 ::inputs
 (fn [db [_ path]]
   (get-in db (into [:profile :inputs] path))))

(reg-sub
 ::preferences
 (fn [db]
   (get-in db [:profile :preferences])))

(reg-sub
 ::api-key
 (fn [db]
   (get-in db [:profile :api-key])))
