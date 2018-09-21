(ns bluegenes.ws.tools
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [clojure.string :refer [blank? join]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(def tools-config "tools/package.json")

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
                 false)}))

(defn installed-tools-list
  "Return a list of the installed tools listed in the package.json file. "
  []
  (let [packages (cheshire/parse-string (slurp tools-config) true)]
    (keys (:dependencies packages))))

(defn tools
  "Format the list of tools as a REST response to our GET."
  [session]
  (let [res
        {:tools
         (reduce
          (fn [tool-list newitem]
            (conj tool-list (get-tool-config (name newitem) (:tool-path env))))
          #{} (installed-tools-list))}]
    (response/ok res)))

(defroutes routes
  ;;returns all available tools installed in the /tools folder
           (GET "/all" session (tools session)))
