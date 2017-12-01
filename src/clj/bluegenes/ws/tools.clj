(ns bluegenes.ws.tools
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [clojure.string :refer [blank? join]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn get-tool-config [tool path]
  (let [name (.getName (io/file tool))]
    {:name name
     :config (cheshire/parse-string (slurp (join "/" [path name "config.json"])))
   }))

(defn tools [session]
  (let [file "resources/public/tools"
        res {:tools
             (reduce
                   (fn [tool-list newitem]
                     (if (.isDirectory (io/file newitem))
                       (conj tool-list (get-tool-config newitem file))
                       tool-list))
                   #{} (.listFiles (io/file file)))}]
    (response/ok res)))

(defroutes routes
           (GET "/all" session (tools session)))
