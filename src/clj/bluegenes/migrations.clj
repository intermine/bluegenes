(ns bluegenes.migrations
  (:require [migratus.core :as migratus]
            [config.core :refer [env]]))

(defn migrate []
  (migratus/migrate
    (let [db-config (:database env)]
      (-> (:migrations env)
          (update-in [:db] assoc
                     :user (:user db-config)
                     :password (:password db-config)
                     :subprotocol (:dbtype db-config)
                     :subname (str "//" (:host db-config) "/" (:dbname db-config)))))))