(ns bluegenes.ws.mymine
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [imcljs.auth :as im-auth]
            [clojure.string :refer [blank?]]
            [cheshire.core :as cheshire :refer [generate-string parse-string]]
            [config.core :refer [env]]
            [ring.util.http-response :as response]
            [hugsql.core :as hugsql]
            [postgre-types.json :refer [add-json-type add-jsonb-type]]
            [bluegenes.mounts :refer [db]]))

(add-json-type generate-string parse-string)

(hugsql/def-db-fns "bluegenes/db/sql/mymine.sql")

(defn store-tree [x]
  (if-let [user-id (-> x :session :identity :id)]
    (store-mymine-tree db {:user-id (read-string user-id)
                           :mine "testmine"
                           :data (-> x :params)})
    (response/unauthorized {:error "Please log in"})))

(defn fetch-tree [x]
  (if-let [user-id (-> x :session :identity :id)]
    {:body (cheshire/parse-string
             (:data (fetch-mymine-tree db {:user-id (read-string user-id)
                                           :mine "testmine"})))}
    (response/unauthorized {:error "Please log in"})))

(defroutes routes
           (POST "/tree" req store-tree)
           (GET "/tree" req fetch-tree))