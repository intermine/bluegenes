(ns bluegenes.components.querybuilder.core
  "Query Spec & core functions"
  (:require
    [clojure.string :as string]
    [clojure.tools.reader.edn :as edn]
    #?(:cljs [cljs.spec :as s]
       :clj [clojure.spec :as s])
    [clojure.tools.reader.edn :as edn]))

; TODO:
; can we get ranges for attributes ? e.g. intron->score - what are its min, average & max ? (could be pre-computed & sent with model)


; 11 most popular query constraints revealed!

(def ops ["=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"])

(s/def :q/op #{"=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"})

(def logicops #{'AND 'OR})

(def alphabet
  (map (comp str char)
    (take 26 (iterate inc 65))))

(def alphabet-symbols (map symbol alphabet))

(defn next-letter [letter]
  (first (rest (drop-while (fn [n] (not= n letter)) alphabet))))

(def alphabet-symbol? (into #{} alphabet-symbols))

(def alphabet? (into #{} alphabet))

(s/def :q/code (comp alphabet? name))

(defn next-code [code]
  (next-letter code))

(defn used-codes
  ([db]
   (map :q/code
     (get-in db [:query-builder :query :q/where]))))

(defn where-tree
  ([{:keys [:q/where]}]
   (reduce
     (fn [r [{:keys [:q/path] :as c} i]]
       (update-in r path (fn [z] (conj (or z []) (assoc c :index i)))))
     {} (map vector where (range)))))

(defn to-list [s]
  (edn/read-string
    (string/upper-case s)))

(defn maybe-unwrap [x]
  (if (and (or (vector? x) (list? x)) (== 1 (count x)))
    (first x)
    x))

(defn group-ands
  "
  This is the existing
  way the old QB does things
  e.g.

  A or B and C or D becomes
  A or (B and C) or D
  "
  [l]
  (maybe-unwrap
    (if (symbol? l)
     l
     (reduce
       (fn [r l]
         (if (= 'AND (second l))
           (conj r (map group-ands l))
           (into r (map group-ands l))))
       [] (partition-by #{'OR} l)))))

(defn nested-infix [[f o & r]]
  (if r
    (list f o (nested-infix r))
    (if o (list f o) f)))

(defn infix-prefix
  "Returns the prefix representation of the given infix expression"
  [x]
  (if (symbol? x)
    x
    (cons (second x)
      (map infix-prefix (take-nth 2 x)))))

(defn simplify
  "Returns "
  ([x]
   (cond
     (symbol? x) x
     (== (count x) 2) (last x)
     (and (> (count x) 1) (#{'AND 'OR 'and 'or} (first x)))
      (simplify x (cons (first x) (remove nil? (map simplify (distinct (rest x))))))
     :and-if-all-else-fails (last x)))
  ([x y]
   (if (= x y)
     x
     (simplify y))))

(defn prefix-infix
  "Returns the infix representation of the given prefix expression"
  [x]
  (if (symbol? x)
    x
    (interpose (first x) (map prefix-infix (rest x)))))

; ------------ spec ---------------

(s/def :q/logicop logicops)

(s/def :q/listy
  (s/or
    :simple alphabet-symbol?
    :complex :q/logic))

(s/def :q/logic
  (s/or
    :exp
      (s/cat
       :op :q/logicop
       :args (s/+ :q/listy))
    :nil nil?))

(s/def :q/infix-list
  (s/or
    :simple alphabet-symbol?
    :complex :q/infix-exp))

(s/def :q/infix-exp
  (s/or
    :exp
      (s/cat
        :a-list :q/infix-list
        :op :q/logicop
        :b-list :q/infix-list)
    :nil nil?))

(s/def :q/path (s/coll-of string?))

(s/def :q/value (s/or :string string? :number number?))

; {:path "Gene.length", :op "<", :value "32"}
(s/def :q/clause
  (s/keys
    :req [:q/path :q/op :q/value :q/code]
    :opt-un [::type]))

(s/def :q/view (s/coll-of string?))

(s/def :q/select
  (s/+ :q/view))

(s/def :q/where
  (s/coll-of :q/clause))

(s/def :q/query
  (s/keys
    :req [:q/select :q/where]
    :opt [:q/logic]
    :opt-un [::constraint-paths]))

(defn ors [where]
  (let [codes (distinct (map :q/code where))]
    (if (> (count codes) 1)
      (apply str
       (interpose " "
         (interpose 'OR codes)))
      "")))

(defn build-query
  "Returns a query for the webservice
  from the given Spec validatable query map"
  ([{:keys [q/select q/where q/logic logic-str] :as query}]
   (-> {}
     (assoc :select
            (map (fn [view] (string/join "." view)) select))
     (assoc :where
            (map (fn [constraint]
                   {
                    :path  (string/join "." (:q/path constraint))
                    :op    (:q/op constraint)
                    :value (:q/value constraint)}) where))
     (assoc :constraintLogic
            (if logic
              logic-str
              (ors where))))))