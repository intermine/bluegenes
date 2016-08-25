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

(def path-types
  {"class"             :class
   "java.lang.String"  :string
   "java.lang.Boolean" :boolean
   "java.lang.Integer" :integer
   "java.lang.Double"  :double
   "java.lang.Float"   :float})

(defn path-type
  "Returns a keyword representing the type of the path
  (see path-types for possible types
  TODO: This is atrocious and needs to be reassessed. But it works."
  [model path & subclasses]
  (cond
    (string? path)
    (recur model (map keyword (clojure.string/split path ".")) subclasses)
    (some (comp string? :path) subclasses)
    (recur model path (map (fn [subclass]
                             (if (string? (:path subclass))
                               (update-in subclass [:path]
                                          (fn [p]
                                            (map keyword (clojure.string/split p "."))))
                               subclass)) subclasses))

    :else (let [[class child & remaining] path]
            (if
              (nil? child)
              :class
              (if-let [child-class (keyword (:referencedType (child (reduce merge (map (class model) [:references :collections])))))]
                (if remaining
                  (let [sc            (if (= child (second (:path (first subclasses))))
                                        (map (fn [x] (update x :path rest)) subclasses)
                                        subclasses)
                        subclass-path (:path (first sc))
                        child-class   (if (and (empty? (rest subclass-path)) (= child (first subclass-path)))
                                        (keyword (:type (first sc)))
                                        child-class)]
                    (recur model (cons child-class remaining) sc))
                  :class)
                (get path-types (:type (child (:attributes (class model))))))))))
