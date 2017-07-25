(ns bluegenes.mounts
  (:require [mount.core :refer [defstate]]
            [hikari-cp.core :refer [make-datasource]]
            [config.core :refer [env]]))

; See config/[env]/config.edn :database
(defstate db :start (let [db-config (:database env)]
                      {:datasource (make-datasource (assoc (:hikari env)
                                                      :username (:user db-config)
                                                      :password (:password db-config)
                                                      :server-name (:host db-config)
                                                      :adapter (:dbtype db-config)
                                                      :port-number (:port db-config)))}))