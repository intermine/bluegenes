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


(defn display-name [model class]
  (get-in model [(if (keyword? class) class (keyword class)) :displayName]))

(defn trim-path-to-class
  "Trims a path string to its parent class.
  Example:
  (trim-path-to-class flymine-model Gene.homologues.homologue.name )
  => Gene.homologues.homologue"
  [model path]
  (let [parts (map keyword (clojure.string/split path "."))]
    (loop [parts-remaining parts
           collected       [(first parts-remaining)]]
      (let [[parent child] parts-remaining]
        (if-let [child-reference-type (keyword (get-in (merge (:references ((keyword parent) model))
                                                              (:collections ((keyword parent) model)))
                                                       [child :referencedType]))]
          (recur (conj (rest (rest parts-remaining)) child-reference-type)
                 (conj collected child))
          (clojure.string/join "." (map name collected)))))))


(defn sterilize-query [query]
  ;(println "sterilizing query" query)
  (update query :select
          (fn [paths]
            ;(println "sees paths" paths)
            (if (contains? query :from)
              (mapv (fn [path]
                      (if (= (:from query) (first (clojure.string/split path ".")))
                        path
                        (str (:from query) "." path))) paths)
              paths))))


(defn referenced-type [model class-kw field-kw]
  (keyword (:referencedType (field-kw (apply merge (map (class-kw model) [:references :collections]))))))


(defn im-type
  "Returns the class that represents a path
  (class Gene.homologues.homologue.symbol) => Gene"
  [model path]
  (let [parts (map keyword (clojure.string/split path "."))]
    (loop [class (first parts)
           parts (rest parts)]
      (let [c (referenced-type model class (first parts))
            remaining (rest parts)]
        (if (nil? c)
          class
          (if (empty? remaining) c (recur c remaining)))))))



(defn get-parts
  "Get all of the different parts of an intermine query and group them by type"
  [model query]
  (group-by :type
            (distinct
              (map (fn [path]
                     (assoc {}
                       :type (im-type model path)
                       :path (str (trim-path-to-class model path) ".id")
                       :query {:select [(str (trim-path-to-class model path) ".id")]
                               :where (:where query)}))
                   (:select query)))))




(defn deconstruct-query
  "Deconstructs a query into a map. Keys are the unique paths
  and values include the query to return the values in the path,
  the display name, and the end class.
  {:Gene.pathways {:end-class Pathway :display-name Pathway :query ...}}"
  [model query]
  (let [sterile-query (sterilize-query query)
        classes       (into [] (comp
                                 (map (partial trim-path-to-class model))
                                 (distinct))
                            (:select sterile-query))]
    (reduce (fn [total next]
              (let [end-class    (end-class model next)
                    display-name (display-name model end-class)]
                (assoc total next
                             {:count        "N"
                              :end-class    end-class
                              :display-name display-name
                              :query        (assoc query :select (str next ".id"))}))) {} classes)))
