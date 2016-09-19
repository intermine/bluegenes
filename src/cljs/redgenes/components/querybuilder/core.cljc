(ns redgenes.components.querybuilder.core
  "Query Spec & core functions"
  (:require
    [clojure.string :as string]
    #?(:cljs [cljs.spec :as s]
       :clj [clojure.spec :as s])))

; TODO:
; tooltips for what icons/buttons mean before clicking
; can we get ranges for attributes ? e.g. intron->score - what are its min, average & max ? (could be pre-computed & sent with model)
; add delete button to eye things


; 11 most popular query constraints revealed!
(def constraints
  {
    := "="
    :!= "!="
    :contains "CONTAINS"
    :< "<"
    :<= "<="
    :> ">"
    :>= ">="
    :like "LIKE"
    :not-like "NOT LIKE"
    :one-of "ONE OF"
    :none-of "NONE OF"})

(s/def :q/op #{"=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"})

(s/def :q/openparen #{"("})

(s/def :q/closeparen #{")"})

(def alphabet
  (map (comp str char)
    (take 26 (iterate inc 65))))

(def next-letter
  (into {} (map vector alphabet (rest (cycle alphabet)))))

(def alphabet? (into #{} alphabet))

(s/def :q/code symbol?)

(defn next-code [code]
  (next-letter code))

(defn used-codes
  ([db]
   (into #{}
     (map :q/code
       (get-in db [:query-builder :query :q/where])))))

; "constraintLogic": "A or B",
; (A OR B) AND (C OR D)

(s/def :q/logicop #{'and 'or 'not})

(s/def :q/logic-expression
  (s/alt
    :complex
    (s/cat
      :expression :q/code
      :logicoperation :q/logicop
      :expression :q/code)
    :simple :q/code))

(s/def :q/logic
  (s/cat
    :expression :q/logic-expression
    :logicoperation :q/logicop
    :expression :q/logic-expression))

(s/def :q/path (s/coll-of string?))

(s/def :q/value (s/or :string string? :number number?))

; {:path "Gene.length", :op "<", :value "32"}
(s/def :q/clause
  (s/keys
    :req [:q/path :q/op :q/value :q/code]))

(s/def :q/view (s/coll-of string?))

(s/def :q/select
  (s/coll-of :q/view))

(s/def :q/where
  (s/coll-of :q/clause))

(s/def :q/query
  (s/keys
    :req [:q/select :q/where]
    :opt [:q/logic]))

(defn build-query
  "What the given query looks like
  now will shock you!"
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
       (assoc :constraintLogic logic-str))))