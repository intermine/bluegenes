(ns bluegenes.utils
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn read-fingerprints
  "Reads manifest.edn which is produced by the ClojureScript compiler's
  `:fingerprint` option. This is a map from target path to the compiled
  path with the fingerprinted filename."
  []
  (try (some->> "public/js/compiled/manifest.edn"
                io/resource
                slurp
                edn/read-string
                ;; Replace backslashes with forward slashes in case of Windows.
                (reduce-kv
                 (fn [m k v]
                   (assoc m
                          (str/replace k #"\\" "/")
                          (str/replace v #"\\" "/")))
                 {}))
       (catch Exception _)))

(defn get-bundle-path
  "Uses the fingerprinted filename if available, and otherwise falls back
  to the default."
  [fingerprints]
  (or (some-> (get fingerprints "resources/public/js/compiled/app.js")
              (str/replace #"resources/public" ""))
      "/js/compiled/app.js"))

(defn parse-bundle-hash
  "Extracts the hash string from a bundle-path."
  [bundle-path]
  (or (second (re-find #"app-(.*)\.js" bundle-path))
      "dev"))

(defn read-bundle-hash
  "Wrapper function for read-fingerprints, get-bundle-path and parse-bundle-hash."
  []
  (-> (read-fingerprints)
      (get-bundle-path)
      (parse-bundle-hash)))

(defn insert-filename-css
  "Rename a path ending with filename 'foo.css' to 'foo-<fingerprint>.css'"
  [file-path fingerprint]
  (str/replace file-path #"\.css$" (str "-" fingerprint ".css")))

(defn env->mines
  "Parses env to return a vector of configured mines.
  Guarantees first mine to always be default."
  [env]
  (concat [{:root (:bluegenes-default-service-root env)
            :name (:bluegenes-default-mine-name env)
            :namespace (:bluegenes-default-namespace env)}]
          (:bluegenes-additional-mines env)))
