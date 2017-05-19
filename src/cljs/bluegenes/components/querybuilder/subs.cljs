(ns bluegenes.components.querybuilder.subs
  "All the subscriptions for the query builder"
  (:require
    [bluegenes.utils :refer [reg-all-subs!]]))

(reg-all-subs!
  [
   [:constraint :query-builder/current-constraint]
   [:counting?]
   [:count]
   [:queried?]
   [:used-codes]
   [:io-query]
   [:query]
   [:constraintLogic]
   ])