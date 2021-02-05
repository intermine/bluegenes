(ns bluegenes.subs.auth
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::auth
 :<- [:current-mine]
 (fn [current-mine]
   (:auth current-mine)))

(reg-sub
 ::identity
 :<- [::auth]
 (fn [auth]
   (:identity auth)))

(reg-sub
 ::superuser?
 :<- [::identity]
 (fn [identity]
   (:superuser identity)))

(reg-sub
 ::authenticated?
 :<- [::identity]
 (fn [identity]
   (some? (not-empty identity))))

(reg-sub
 ::email
 :<- [::identity]
 (fn [identity]
   ;; Set when using OAuth2, where username is just "GOOGLE:gibberish".
   (or (not-empty (get-in identity [:preferences :email]))
       (:username identity))))
