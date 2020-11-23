(ns bluegenes.prep
  "This namespace is called by leiningen before `bluegenes.core` to perform
  some preparation tasks prior to starting BlueGenes.
  Do not require this namespace and use it from the rest of the project!"
  (:require [clojure.java.io :as io]))

(defn copy-im-tables-css []
  (let [source-file "public/css/im-tables.css"
        target-file "resources/public/css/im-tables.css"]
    ;; css dir is missing after a lein clean.
    (io/make-parents target-file)
    (->> (io/resource source-file)
         (slurp)
         (spit target-file))))

(defn prepare-assets []
  (copy-im-tables-css))
