(ns bluegenes.pages.reportpage.utils
  (:require [clojure.string :as str]))

(defn strip-class
  "Removes everything before the first dot in a path, effectively removing the root class.
  (strip-class `Gene.organism.name`)
  => `organism.name`"
  [path-str]
  (second (re-matches #"[^\.]+\.(.*)" path-str)))

;; We don't use the `imcljs.path` functions here as we're manually building a
;; path by appending a tail to a different root.
(defn ->query-ref+coll [summary-fields object-type object-id ref+coll]
  (let [{:keys [name referencedType]} ref+coll]
    {:from object-type
     :select (->> (get summary-fields (keyword referencedType))
                  (map strip-class)
                  (map (partial str object-type "." name ".")))
     :where [{:path (str object-type ".id")
              :op "="
              :value object-id}]}))
