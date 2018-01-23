(ns bluegenes.utils
  (:require [clojure.string :refer [blank? join split upper-case]]))

(def not-blank? (complement blank?))

(defn uncamel
  "Uncamel case a string. Example: thisIsAString -> This is a string"
  [s]
  (if (not-blank? s)
    (join (-> (split (join " " (split s #"(?=[A-Z][^A-Z])")) "")
              (update 0 upper-case)))
    s))