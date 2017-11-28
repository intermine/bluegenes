(ns bluegenes.ws.tools
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :as route]
            [clojure.string :refer [blank?]]
            [cheshire.core :as cheshire]
            [config.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]            ))

(defn tools [session]
 (let [file "resources/public/tools"
       res { 
  ;;  :file (load-file file)
    :tools (str (reduce 
           (fn [coll newitem] 
            (if (.isDirectory (io/file newitem))
            (conj coll newitem)
            coll
            ))
           (.listFiles (io/file file)))      )     
      }]
  (pr-str res
  
           )))

(defroutes routes
           (GET "/all" session (tools session)))