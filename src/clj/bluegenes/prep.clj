(ns bluegenes.prep
  "This namespace is called by leiningen before `bluegenes.core` to perform
  some preparation tasks prior to starting BlueGenes.
  Do not require this namespace and use it from the rest of the project!"
  (:require [clojure.java.io :as io]
            [taoensso.timbre :refer [error]]
            [bluegenes.utils :as utils]))

(def bluegenes-css "public/css/site.css")
(def im-tables-css "public/css/im-tables.css")

(defn resource-path [path]
  (str "resources/" path))

(defn copy-im-tables-css []
  (let [source-file im-tables-css
        target-file (resource-path im-tables-css)]
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

;; Below is called directly as they depend on files that come later in the
;; build process.
(defn fingerprint-css []
  (let [fingerprint (utils/read-bundle-hash)]
    (assert (not= fingerprint "dev") "fingerprint-css should only be run for a production build")
    (doseq [css-file [bluegenes-css im-tables-css]]
      (let [css-file (resource-path css-file)]
        (io/copy (io/file css-file)
                 (io/file (utils/insert-filename-css css-file fingerprint)))))))

