(ns redgenes.components.querybuilder.core
  "Query Spec & core functions"
  (:require
    [clojure.string :as string]
    [clojure.tools.reader.edn :as edn]
    #?(:cljs [cljs.spec :as s]
       :clj [clojure.spec :as s])
    [clojure.tools.reader.edn :as edn]))

; TODO:
; tooltips for what icons/buttons mean before clicking
; can we get ranges for attributes ? e.g. intron->score - what are its min, average & max ? (could be pre-computed & sent with model)
; add delete button to eye things


; 11 most popular query constraints revealed!

(def ops ["=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"])

(s/def :q/op #{"=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"})

(def logicops #{'AND 'OR})

(s/def :q/openparen #{"("})

(s/def :q/closeparen #{")"})

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
     (fn [r {:keys [:q/path] :as c}]
       (update-in r path (fn [z] (conj (or z []) c))))
     {} where)))

(defn to-list [s]
  (edn/read-string
    (string/upper-case s)))

(defn maybe-unwrap [x]
  (if (and (or (vector? x) (list? x)) (== 1 (count x)))
    (first x)
    x))

(defn group-ands [l]
  (maybe-unwrap
    (if (symbol? l)
     l
     (reduce
       (fn [r l]
         (if (= 'AND (second l))
           (conj r (map group-ands l))
           (into r (map group-ands l))))
       [] (partition-by #{'OR} l)))))

(defn to-prefix [x]
  (if (symbol? x)
    x
    (cons (second x)
      (map to-prefix (take-nth 2 x)))))

(defn simplify
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
  [x]
  (if (symbol? x)
    x
    (interpose (first x) (map prefix-infix (rest x)))))

; "constraintLogic": "A or B",
; (A OR B) AND (C OR D)

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

(s/def :q/path (s/coll-of string?))

(s/def :q/value (s/or :string string? :number number?))

; {:path "Gene.length", :op "<", :value "32"}
(s/def :q/clause
  (s/keys
    :req [:q/path :q/op :q/value :q/code]))

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

(defn maybe-with-logic
  [s logic-str]
  (if (= "" logic-str)
    s
    (assoc s :constraintLogic logic-str)))

(defn build-query
  "Returns a query for the webservice"
  ([{:keys [q/select q/where logic-str] :as query}]
   (-> {}
     (assoc :select
            (map (fn [view] (string/join "." view)) select))
     (assoc :where
            (map (fn [constraint]
                   {
                    :path  (string/join "." (:q/path constraint))
                    :op    (:q/op constraint)
                    :value (:q/value constraint)}) where))
     (maybe-with-logic logic-str))))