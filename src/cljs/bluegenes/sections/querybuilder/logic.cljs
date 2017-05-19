(ns bluegenes.sections.querybuilder.logic
  (:require [re-frame.core :refer [subscribe dispatch]]


            [clojure.string :refer [split join blank? starts-with? ends-with?]]
            [oops.core :refer [ocall oget]]
            [cljs.reader :refer [read-string]]
            [cljs.reader :refer [read-string]]))




(defn index-of [haystack needle]
  (first (keep-indexed (fn [idx e] (when (= needle e) idx)) haystack)))

(defn group-ands
  "Recurisvely groups entities in a vector that are connected by the 'and symbol"
  [v]
  (if (vector? v)
    (let [first-part (vec (take (dec (index-of v 'and)) v))
          grouped    (vec (take-while (partial not= 'or) (drop (count first-part) v)))
          end        (take-last (- (count v) (+ (count first-part) (count grouped))) v)
          grouped    (if (= 1 (count grouped)) (first grouped) grouped)]
      (let [final (reduce conj (conj first-part grouped) end)]
        (if (index-of final 'and) (recur final) final)))
    (vector v))
  )

(defn without-operators [col]
  (vec (filter (fn [item] (not (some? (some #{item} #{'and 'or})))) col)))

(defn single-vec-of-vec? [item]
  "Is the item a vector containing one vector? [[A]]"
  (and (= (count item) 1) (vector? (first item))))

(defn single-vec-of-symbol? [item]
  "Is the item a vector containing a symbol? ['A]"
  (and (= (count item) 1) (symbol? (first item))))



(defn raise [v]
  (if (and (vector? v) (< (count v) 2) (vector? (first v)))
    (recur (first v))
    v))

(defn vec->list
  "Recursively convert vectors to lists"
  [v]
  (clojure.walk/postwalk (fn [e] (if (vector? e) (apply list e) e)) v))

(defn list->vec
  "Recursively convert lists to vectors"
  [v]
  (clojure.walk/postwalk (fn [e] (if (list? e) (vec e) e)) v))

(defn add-left-bracket [s] (str "[" s))
(defn add-right-bracket [s] (str s "]"))

(defn wrap-string-in-brackets [s]
  (cond-> s
          (not (starts-with? s "(")) add-left-bracket
          (not (ends-with? s ")")) add-right-bracket))

(defn clause-type [v]
  (cond
    (some? (some #{'or} v)) :or
    (some? (some #{'and} v)) :and
    :else nil))

(defn append-code [v code]
  (if (empty? v)
    [code]
    (do
      (let [type (clause-type v)]
        (-> (case type
              :or (reduce conj [] [v 'and code])
              (reduce conj v ['and code]))
            group-ands
            raise)))))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))


(defn remove-repeats [v]
  (let [without-repeats (reduce (fn [total next] (if (not= next (last total)) (conj total next) total)) [] v)]
    (vec (cond->> without-repeats
              (one-of? ['or 'and] (first without-repeats)) (drop 1)
              (one-of? ['or 'and] (last without-repeats)) butlast))))

(defn remove-code
  "Recursively removes a symbol from a tree and raises neighbours with a count of one"
  [v code]
  (->
    (clojure.walk/postwalk
      (fn [e]
        (if (vector? e)
          (let [removed     (vec (remove (partial = code) e))
                without-ops (without-operators removed)]
            (cond
              (single-vec-of-vec? without-ops) (vec (mapcat identity without-ops))
              (single-vec-of-symbol? without-ops) (first without-ops)
              :else (remove-repeats removed)))
          e))
      v)
    group-ands
    raise))

(defn read-logic-string [s]
  (some-> s
          wrap-string-in-brackets
          read-string
          list->vec
          group-ands
          raise))