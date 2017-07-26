(ns bluegenes.auth
  (:require [buddy.hashers :as hs]
            [bluegenes.db.users :as queries]
            [buddy.sign.jwt :as jwt]
            [config.core :refer [env]]))

(defn encrypt [password] (hs/encrypt password))
(defn check [hash] (hs/check hash))

(defn store-user! [{:keys [password] :as user}]
  (try
    (-> user
        (assoc :hash (encrypt password))
        (dissoc :password)
        queries/store-user!)
    (catch Exception e (do (println "ERROR") {:error :duplicate}))))

(defn authenticate-user [email password]
  (if-some [{:keys [hash] :as user} (queries/first-user-by-email email)]
    (when (hs/check password hash)
      (let [stripped (dissoc user :hash)]
        (assoc stripped :token (jwt/sign stripped (:signature env)))))))