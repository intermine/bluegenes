(ns bluegenes.auth
  (:require [buddy.hashers :as hs]
            [bluegenes.db.users :as queries]
            [buddy.sign.jwt :as jwt]
            [config.core :refer [env]]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [clojure.string :refer [blank?]]
            ))

(defn store-user! [{:keys [password] :as user}]
  (try
    ; Store the user in the database and strip password on returned value
    (let [stored-user (-> user
                          (assoc :password (hs/encrypt password))
                          queries/store-user!
                          (dissoc :password))]
      ; Give the new user a JWT
      (assoc stored-user :token (jwt/sign stored-user (:signature env))))
    ; If there was an error then the user probably already exists
    ; TODO - how do we specifically catch a duplication error?
    (catch Exception e {:error :duplicate})))

(defn authenticate-user [username password]
  ; Find the user by their username
  (if-some [{hashed-password :password :as user} (queries/first-user-by-name username)]
    ; If the hashed passwords match then return the user sans password with JWT
    (when (hs/check password hashed-password)
      (let [stripped (dissoc user :password)]
        (assoc stripped :token (jwt/sign stripped (:signature env)))))))


(defn fetch-token
  [{:keys [username password token]}]
  (let [resp (auth/basic-auth {:root "www.flymine.org/query"} username password)]
    (when (not (blank? resp)) resp)))