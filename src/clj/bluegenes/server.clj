(ns bluegenes.server
  (:require [bluegenes.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [bluegenes.mounts :as mounts]
            [mount.core :as mount]
            [taoensso.timbre :as timbre :refer [infof errorf]]
            [bluegenes.migrations :as migrations])
  (:gen-class))

(defn ->int
  "Force a value to a number (environment variables are read as strings)"
  [n]
  (cond
    (string? n) (read-string n)
    (int? n) n
    :else n))

(defn -main [& args]
  (let [port (->int (or (:server-port env) 5000))]
    (timbre/set-level! :info)
    (try
      (do
        (mount/start)
        (migrations/migrate))
      (catch Exception e (errorf "Unable to connect to database: %s" (.getMessage e))))
    (run-jetty handler {:port port :join? false})
    (infof "Bluegenes server started on port: %s" port)))
