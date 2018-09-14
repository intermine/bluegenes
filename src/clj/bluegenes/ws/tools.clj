(ns bluegenes.ws.tools
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [clojure.string :refer [blank? join]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn get-tool-config
  "check tool folder for config and other relevant files and return as
   a map of useful info. This is used client-side by the browser to
   load tools relevant for a given report page."
  [tool path]
  (let [name (.getName (io/file tool))
        path (join "/" [path name])
        ;; this is the bluegenes-specific config.
        bluegenes-config (str path "/config.json")
        ;;this is the default npm package file
        package (str path "/package.json")
        ;;optional preview image for each tool.
        preview-image "/preview.png"
        browser-path (str "/tools/" name preview-image)]
    {:name name
      :config (cheshire/parse-string (slurp bluegenes-config))
      :package (cheshire/parse-string (slurp package))
      ;; return image path if it exists, or false otherwise.
      :hasimage (if (.exists (io/file (str path preview-image)))
                  browser-path
                  false)
   }))

(defn tools [session]
  (let [file "tools"
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
