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
  (let [name (.getName (io/file tool))
        path (join "/" [path name])
        bluegenes-config (str path "/config.json")
        package (str path "/package.json")
        preview-image "/preview.png"
        browser-path (str "/tools/" name preview-image)]
    {:name name
      :config (cheshire/parse-string (slurp bluegenes-config))
      :package (cheshire/parse-string (slurp package))
      :hasimage (if (.exists (io/file (str path preview-image)))
                  browser-path
                  false)
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
