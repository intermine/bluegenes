(ns bluegenes.core
  (:require [bluegenes.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :as timbre :refer [infof]]
            [bluegenes-tool-store.tools :refer [initialise-tools]])
  (:import [org.eclipse.jetty.server.handler.gzip GzipHandler])
  (:gen-class))

(defn ->int
  "Force a value to a number (environment variables are read as strings)"
  [n]
  (cond
    (string? n) (read-string n)
    (int? n) n
    :else n))

(defn add-gzip-handler [server]
  (.setHandler server
               (doto (GzipHandler.)
                 (.setIncludedMimeTypes (into-array ["text/css"
                                                     "application/javascript"
                                                     "text/javascript"]))
                 (.setMinGzipSize 1024)
                 (.setHandler (.getHandler server)))))

(defonce web-server_ (atom nil))
(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))
(defn start-web-server!
  "Parses the port from the configuration file, environment variables, or default to 5000
  (\"PORT\" is often the default value for app serving platforms such as Heroku and Dokku)
  and start the Jetty server by passing in the URL routes defined in `handler`."
  []
  (stop-web-server!)
  (let [port    (->int (or (:server-port env) (:port env) 5000))
        server  (run-jetty handler (merge
                                    {:port port :join? false}
                                    (when-not (:development env)
                                      {:configurator add-gzip-handler})))
        stop-fn #(.stop server)]
    (infof "=== Bluegenes server started on port: %s" port)
    (reset! web-server_ stop-fn)))

(defn -main
  "Start the BlueGenes server. This is the main entry point for the application"
  [& _args]
  (timbre/set-level! (keyword (:logging-level env :info)))
  (initialise-tools)
  (start-web-server!))
