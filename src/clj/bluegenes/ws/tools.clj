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
  (let [tool-name (subs (str tool) 1)
        path (join "/" [path tool-name])
        ;; this is the bluegenes-specific config.
        bluegenes-config-path (str path "/config.json")
        config (cheshire/parse-string (slurp bluegenes-config-path) true)
        ;;this is the default npm package file
        package-path (str path "/package.json")
        package (cheshire/parse-string (slurp package-path) true)
        ;;optional preview image for each tool.
        preview-image "/preview.png"
        browser-path (str "/tools/" tool-name preview-image)]
    ;; so many naming rules that conflict - we need three names.
    ;; npm requires kebab-case bluegenes-tool-protvista
    ;; but js vars forbid kebab case bluegenesToolProtvista
    ;; humans want something with spaces "Protein viewer"
    ;; this is terminally incompatible, hence three names. Argh.
    {:names {:human (get-in config [:toolName :human])
             :cljs (get-in config [:toolName :cljs])
             :npm (get-in package [:name])}
     :config config
     :package package
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
            (conj tool-list (get-tool-config newitem (:bluegenes-tool-path env))))
          #{} (installed-tools-list))}]
    (response/ok res)))

(defroutes routes
  ;;returns all available tools installed in the /tools folder
           (GET "/all" session (tools session)))
