(ns bluegenes.ws.mymine
  (:require [compojure.core :refer [GET POST DELETE defroutes]]
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
            [clojure.string :as s]
            [hugsql.parameters :refer [identifier-param-quote]]))

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

(defn toggle-open [entry-id status req]
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    {:body (mymine-entry-update-open db {:entry-id entry-id :open status})}
    (response/unauthorized {:error "Unauthorized"})))

(defn update-label [entry-id label req]
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    (do
      (println "label2" label)
      {:body (map lodash-to-hyphen (mymine-entry-update-label db {:entry-id entry-id :label label}))})
    (response/unauthorized {:error "Unauthorized"})))

(defn delete-tag [entry-id req]
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    {:body (map lodash-to-hyphen (mymine-entry-delete-entry db {:entry-id entry-id}))}
    (response/unauthorized {:error "Unauthorized"})))

(defn move-tag [id pid req]
  (println "TEST" id pid)
  (if-let [user-id (parse-string (-> req :session :identity :id))]
    {:body (map lodash-to-hyphen (mymine-move-entry db {:entry-id id :parent-id pid}))}
    (response/unauthorized {:error "Unauthorized"})))

(defroutes routes
           ; Create a new entry
           (POST "/entries" req add-entry)
           ; Store whether or a tag is expanded or not
           (POST "/entries/:id/open/:status" [id status :as req] (toggle-open id status req))
           ; ;;;;
           (POST "/entries/:id/rename/:label" [id label :as req] (update-label id label req))
           ; Move entry to the root level
           (POST "/entries/:id/move/" [id :as req] (move-tag id nil req))
           ; Move entry under a new location
           (POST "/entries/:id/move/:pid" [id pid :as req] (move-tag id pid req))
           ; Move an untagged item to a new one
           #_(POST "/entries//move/:pid" [pid :as req] (move-tag2 id pid req))
           ; Delete an entry
           (DELETE "/entries/:id" [id :as req] (delete-tag id req))
           ; Get all entries
           (GET "/entries" req get-entries))

