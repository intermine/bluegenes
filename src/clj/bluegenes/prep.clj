(ns bluegenes.prep
  "This namespace is called by leiningen before `bluegenes.core` to perform
  some preparation tasks prior to starting BlueGenes.
  Do not require this namespace and use it from the rest of the project!"
  (:require [clojure.java.io :as io]))

(defn copy-im-tables-css []
  (->> (io/resource "public/css/im-tables.css")
       (slurp)
       (spit "resources/public/css/im-tables.css")))

(defn prepare-assets []
  (copy-im-tables-css))
