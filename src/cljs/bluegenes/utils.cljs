(ns bluegenes.utils
  (:require [clojure.string :refer [blank? join split capitalize]]))

(defn uncamel
  "Uncamel case a string. Example: thisIsAString -> This is a string"
  [s]
  (if-not (blank? s)
    (as-> s $
          (split $ #"(?=[A-Z][^A-Z])")
          (join " " $)
          (capitalize $))
    s))