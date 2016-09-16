(ns redgenes.components.querybuilder.core
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

(s/def :q/code (into #{} (map (comp str char) (take 26 (iterate inc 65)))))

(s/def :q/logicop #{"and" "or" "not"})

(s/def :q/lc
  (s/alt
    :complex (s/cat :pair (s/+ (s/cat :code :q/code :logic :q/logicop)) :code :q/code)
    :simple :q/code))

(s/def :q/llc
  (s/cat :open :q/openparen ))

(s/def :q/logic string?)

(s/def :q/path (s/coll-of string?))

; "constraintLogic": "A or B",
; (A OR B) AND (C OR D)

(s/def :q/code string?)

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
    :opt [:q/thing]))

(defn build-query
  "What the given query looks like
  now will shock you!"
  ([{:keys [q/select q/where] :as query}]
    (-> {}
      (assoc :select
             (map (fn [view] (string/join "." view)) select))
      (assoc :where
             (map (fn [constraint]
                    {
                     :path  (string/join "." (:q/path constraint))
                     :op    (:q/op constraint)
                     :value (:q/value constraint)}) where)))))