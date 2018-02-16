(ns bluegenes.core
  (:require [bluegenes.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :as timbre :refer [infof errorf]]
            [mount.core :as mount]
    ; Including this namespace establishes the database connection when (mount/start) is called:
            [bluegenes.mounts :as mounts]
            [bluegenes.migrations :as migrations])
  (:gen-class))

(defn ->int
  "Force a value to a number (environment variables are read as strings)"
  [n]
  (cond
    (string? n) (read-string n)
    (int? n) n
    :else n))

(defn -main
  "Start the BlueGenes server. This is the main entry point for the application"
  [& args]
  ; Parse the port from the configuration file, environment variables, or default to 5000
  ; "PORT" is often the default value for app serving platforms such as Heroku and Dokku
  (let [port (->int (or (:server-port env) (:port env) 5000))]
    (timbre/set-level! :info) ; Enable Logging
    #_(try
      (do
        (mount/start) ; Mount our database connection
        (migrations/migrate)) ; Apply any database migrations that haven't been applied
      (catch Exception e (errorf "Unable to connect to database: %s" (.getMessage e))))
    ; Start the Jetty server by passing in the URL routes defined in 'handler'
    (run-jetty handler {:port port :join? false})
    (infof "Bluegenes server started on port: %s" port)))
