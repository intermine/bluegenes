(ns bluegenes.utils
  (:require [clojure.string :refer [blank? join split capitalize split]]))

(defn uncamel
  "Uncamel case a string. Example: thisIsAString -> This is a string"
  [s]
  (if-not (blank? s)
    (as-> s $
      (split $ #"(?=[A-Z][^A-Z])")
      (join " " $)
      (capitalize $))
    s))

(defn read-origin
  "Read the origin class from a query, and infer it if it's missing."
  [query]
  (if-let [origin (:from query)]
    origin
    (first (split (first (:select query)) #"\."))))

(defn kw->str
  [kw]
  (if (keyword? kw)
    (str (namespace kw)
         (when (namespace kw) "/")
         (name kw))
    (do (assert (string? kw) "This function takes only a keyword or string.")
        kw)))

(defn read-registry-mine
  "Grab the most important data from a mine object retrieved from the registry.
  This is how a mine is initially created in `(:mines app-db)`, before it is
  populated with the responses from fetching assets."
  [reg-mine]
  {:service {:root (:url reg-mine)}
   :name (:name reg-mine)
   :id (-> reg-mine :namespace keyword)
   :logo (-> reg-mine :images :logo)})
