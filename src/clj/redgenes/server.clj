(ns redgenes.server
  (:require [redgenes.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [redgenes.core :as rg])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (env :port) "3000"))]
     (run-jetty handler {:port port :join? false})))

(defn thingy2 [] (rg/thingy))
