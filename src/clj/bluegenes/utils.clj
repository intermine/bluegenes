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

(defn get-service-root [env]
  (or (not-empty (:bluegenes-backend-service-root env))
      (:bluegenes-default-service-root env)))

(defn env->mines
  "Parses env to return a vector of configured mines.
  Guarantees first mine to always be default."
  [env]
  (concat [{:root (get-service-root env)
            :name (:bluegenes-default-mine-name env)
            :namespace (:bluegenes-default-namespace env)}]
          (:bluegenes-additional-mines env)))

(defn- timeout
  [req]
  (assoc req
         :socket-timeout 3000
         :connection-timeout 3000))

(defn wrap-timeout
  "Middleware which adds a short timeout to the request."
  [client]
  (fn
    ([req]
     (client (timeout req)))
    ([req respond raise]
     (client (timeout req) respond raise))))

(defn- accept-all
  [req]
  (-> req
      (dissoc :as)
      (assoc :accept "*/*")))

(defn wrap-accept-all
  "Middleware setting the accept header in a request to allow all responses
  and disable output coercion."
  [client]
  (fn
    ([req]
     (client (accept-all req)))
    ([req respond raise]
     (client (accept-all req) respond raise))))
