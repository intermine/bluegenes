(ns bluegenes.ws.mymine
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [imcljs.auth :as im-auth]
            [clojure.string :refer [blank?]]
            [cheshire.core :as cheshire :refer [generate-string parse-string]]
            [config.core :refer [env]]
            [ring.util.http-response :as response]
            [hugsql.core :as hugsql]
            [clojure.walk :as walk]
            [postgre-types.json :refer [add-json-type add-jsonb-type]]
            [bluegenes.mounts :refer [db]]
            [clojure.string :as s]))

(add-json-type generate-string parse-string)

(hugsql/def-db-fns "bluegenes/db/sql/mymine.sql")

(defn lodash-to-hyphen
  "Recursively replace _ with - in a map's keywords"
  [m]
  (walk/postwalk #(if (keyword? %) (-> % name (s/replace #"_" "-") keyword) %) m))

(defn add-entry [req]
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    (let [{:keys [im-obj-type im-obj-id parent-id label]} (:params req)]
      {:body (map lodash-to-hyphen (mymine-add-entry db {:user-id user-id
                                                         :mine "cow"
                                                         :im-obj-type im-obj-type
                                                         :im-obj-id im-obj-id
                                                         :parent-id parent-id
                                                         :label label
                                                         :open? true}))})
    (response/unauthorized {:error "Unauthorized"})))

(defn get-entries [req]
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    {:body (map lodash-to-hyphen (mymine-fetch-all-entries db {:user-id user-id :mine "cow"}))}
    (response/unauthorized {:error "Unauthorized"})))

(defroutes routes
           (POST "/tree" req add-entry)
           (GET "/tree" req get-entries))