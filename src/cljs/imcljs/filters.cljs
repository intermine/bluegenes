(ns imcljs.filters
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [ajax.core :refer [GET POST]]
            [cljs-http.client :as http]
            [imcljs.utils :as utils :refer [cleanse-url]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(defn end-class
  "Returns the deepest class from a query path.
  Example:
  (end-class Gene.proteins flymine-model) ;; Protein
  (end-class Gene.goAnnotation.ontologyTerm.name ;; OntologyTerm"
  [model path]
  (let [parts (map keyword (clojure.string/split path "."))]
    (loop [class           (first parts)
           parts-remaining (rest parts)]
      (if-let [found (get-in model [class])]
        (if (empty? parts-remaining)
          (name class)
          (let [related (merge (:references found) (:collections found))
                {refType :referencedType} (get-in related [(first parts-remaining)])]
            (if (nil? refType)
              (name class)
              (recur (keyword refType) (rest parts-remaining)))))))))

(defn templates-constrained-by-type [model templates]
  (println "END CLASS" (end-class model "Gene.goAnnotation.ontologyTerm.name")))