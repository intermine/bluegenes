(ns bluegenes.mounts
  (:require [mount.core :refer [defstate]]
            [hikari-cp.core :refer [make-datasource]]
            [config.core :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre :refer [infof]]
            [clojure.string :refer [split replace]]
            [clojure.set :refer [rename-keys]])
  (:import [java.net URI]))

(defn parse-database-url
  "Parse a string representing a database URL. Returns nil if not present in env or config."
  [env]
  (when-let [database-url (:database-url env)]
    ; Parse the URL into parts parse-properties-uri is private, work around: reference by symbol!
    (let [{:keys [subname subprotocol user username password]} (#'jdbc/parse-properties-uri (URI. database-url))]
      ; Split the URL into [host:port dn-name]
      (let [[host-and-port name] (-> subname (replace #"//" "") (split #"/"))]
        ; Parse a port (which might not be present
        (let [[host port] (split host-and-port #":")]
          {:db-subname subname
           :db-subprotocol subprotocol
           :db-name name
           :db-host host
           :db-port (or port 5432) ; Default Postgres port if missing
           :db-username (or user username) ; Usernames before the host come back as user, url params as username
           :db-password password})))))

(defn gather-db-config
  "Looks for database configuration. First in environment variabels, then in local config.
  As per the norm, a DATABASE_URL (:database-url) variable overrides other db config"
  [env]
  (if-let [db-spec (parse-database-url env)]
    db-spec
    (select-keys env [:db-subname :db-subprotocol :db-name :db-host :db-port :db-username :db-password])))


(defn hikarify-db-spec
  "Re-key our found configuration variables to those recognized by Hikari
  https://github.com/tomekw/hikari-cp
  https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby"
  [db-spec]
  (-> db-spec
      ; Hikari doesn't like extra keys so pick the ones we want
      (select-keys [:db-host :db-name :db-port :db-username :db-password])
      (rename-keys {:db-host :server-name
                    :db-name :database-name
                    :db-username :username
                    :db-password :password
                    :db-port :port-number})
      (assoc :adapter "postgresql"
             :ssl-mode "disable")))


; See config/[env]/config.edn
; This stateful binding becomes our database connection when (mount/start) is called from the bluegenes.server namespace
(defstate db :start (let [db-config (gather-db-config env)]
                      (infof "Starting DB connection pool to //%s:%s/%s"
                             (:db-host db-config) (:db-port db-config) (:db-name db-config))
                      {:datasource (make-datasource (hikarify-db-spec db-config))}))