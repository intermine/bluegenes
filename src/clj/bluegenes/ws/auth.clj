(ns bluegenes.ws.auth
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [bluegenes.auth :as auth]
            [clojure.string :refer [blank?]]
            [ring.util.http-response :refer [ok bad-request unauthorized]]))

(def not-blank? (complement blank?))

(def valuable? (fn [v] (and (some? v) (not-blank? v))))

(defn handle-registration [{:keys [username password]}]
  (if (not-every? valuable? [username password])
    (bad-request {:response nil})
    (let [new-user (auth/store-user!
                     {:username    username
                      :password password
                      :role     "user"})]
      (if (contains? new-user :error)
        (bad-request new-user)
        (ok new-user)))))

(defn handle-authentication [{:keys [username password] :as x}]
  (if (not-every? valuable? [username password])
    (unauthorized {:user nil})
    (let [user (auth/authenticate-user username password)]
      (if user
        (ok user)
        (unauthorized {:user user})))))

(defroutes routes
           (POST "/login" {params :params} (handle-authentication params))
           (POST "/register" {params :params} (handle-registration params)))