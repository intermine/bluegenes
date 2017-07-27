(ns bluegenes.auth
  (:require [buddy.hashers :as hs]
            [bluegenes.db.users :as queries]
            [buddy.sign.jwt :as jwt]
            [config.core :refer [env]]))

(defn encrypt [password] (hs/encrypt password))
(defn check [hash] (hs/check hash))

(defn store-user! [{:keys [password] :as user}]
  (try
    (let [stored-user (-> user
                          (assoc :password (encrypt password))
                          queries/store-user!
                          (dissoc :password))]
      (assoc stored-user :token (jwt/sign stored-user (:signature env))))
    (catch Exception e {:error :duplicate})))

(defn authenticate-user [username password]
  (if-some [{hashed-password :password :as user} (queries/first-user-by-name username)]
    (when (hs/check password hashed-password)
      (let [stripped (dissoc user :password)]
        (assoc stripped :token (jwt/sign stripped (:signature env)))))))