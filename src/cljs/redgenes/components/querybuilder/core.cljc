(ns redgenes.components.querybuilder.core
  (:require
    #?(:cljs [cljs.spec :as s]
       :clj [clojure.spec :as s])))

; TODO:
; tooltips for what icons/buttons mean before clicking
; can we get ranges for attributes ? e.g. intron->score - what are its min, average & max ? (could be pre-computed & sent with model)
; add delete button to eye things

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

(s/def :query-constraint/op #{"=" "!=" "CONTAINS" "<" "<=" ">" ">=" "LIKE" "NOT LIKE" "ONE OF" "NONE OF"})

(s/def :query/clause string?)

(s/def :query/view string?)

(s/def :query/select
  (s/coll-of :kind :query/view))

(s/def :query/where
  (s/coll-of :kind :query/clause))

(s/def :query/query
  (s/keys
    :req [:query/select :query/where]
    :opt [:query/thing]))


