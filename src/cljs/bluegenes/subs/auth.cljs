(ns bluegenes.subs.auth
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::auth
 (fn [db]
   (get-in db [:auth])))

(reg-sub
 ::identity
 (fn [db]
   (get-in db [:auth :identity])))

(reg-sub
 ::authenticated?
 :<- [::identity]
 (fn [identity]
   (some? (not-empty identity))))