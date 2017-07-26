(ns bluegenes.ws.auth
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [bluegenes.auth :as auth]
            [clojure.string :refer [blank?]]
            [ring.util.http-response :refer [ok bad-request unauthorized]]))

(def not-blank? (complement blank?))

(defn handle-registration [{:keys [email password]}]
  (if (not-every? some? [email password])
    (bad-request {:response nil})
    (let [new-user (auth/store-user!
                     {:email    email
                      :password password
                      :role     "user"})]
      (if (contains? new-user :error)
        (bad-request new-user)
        (ok new-user)))))

(defn handle-authentication [{:keys [email password]}]
  (if (not-every? not-blank? [email password])
    (unauthorized {:user nil})
    (let [user (auth/authenticate-user email password)]
      (if user
        (ok user)
        (unauthorized {:user user})))))

(defroutes routes
           (POST "/login" {params :params} (handle-authentication params))
           (POST "/register" {params :params} (handle-registration params)))