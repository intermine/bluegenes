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
