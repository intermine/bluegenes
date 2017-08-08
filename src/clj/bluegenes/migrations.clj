(ns bluegenes.migrations
  (:require [migratus.core :as migratus]
            [config.core :refer [env]]
            [bluegenes.mounts :refer [gather-db-config]]
            [clojure.set :refer [rename-keys]]))

(defn migratusify-db-spec
  "Re-key our found configuration variables to those recognized by Migratus
  https://github.com/yogthos/migratus"
  [db-spec]
  (-> db-spec
      (rename-keys {:db-subname :subname
                    :db-username :user
                    :db-password :password
                    :db-subprotocol :subprotocol})
      (assoc :classname "org.postgresql.Driver")))

(defn migrate []
  (migratus/migrate
    (let [db-config (gather-db-config env)]
      (-> {:store :database
           :migration-dir "migrations/"
           :migration-table-name "migrations"}
          (assoc :db (migratusify-db-spec (gather-db-config env)))))))