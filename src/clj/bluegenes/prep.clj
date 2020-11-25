(ns bluegenes.prep
  "This namespace is called by leiningen before `bluegenes.core` to perform
  some preparation tasks prior to starting BlueGenes.
  Do not require this namespace and use it from the rest of the project!"
  (:require [clojure.java.io :as io]
            [taoensso.timbre :refer [error]]))

(defn copy-im-tables-css []
  (let [source-file "public/css/im-tables.css"
        target-file "resources/public/css/im-tables.css"]
    ;; css dir is missing after a lein clean.
    (io/make-parents target-file)
    (try
      (->> (io/resource source-file)
           (slurp)
           (spit target-file))
      (catch IllegalArgumentException _
        (error "Failed to read CSS from im-tables dependency. You are likely trying to use an older im-tables version which requires manual copying of its CSS into BlueGenes. If you don't do this now, im-tables will look weird!")))))

(defn prepare-assets []
  (copy-im-tables-css))
