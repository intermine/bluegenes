(ns bluegenes.specs
  "Provides shapes of InterMine and BlueGenes data"
  (:require [clojure.spec.alpha :as s]))

; Note: It's really worth revisiting this in the future. Creating specs for InterMine
; related things like queries, data models, report page widgets, etc can be REALLY powerful
; because we can programmatically capture why something doesn't conform *before* it throws an error.
;
; Creating a spec for data that moves in and out of report page widgets is a really good place to start.

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(s/def :package/type (s/and keyword? (partial one-of? [:query])))
(s/def :package/value some?)
(s/def :package/source keyword?)

(comment
  "The smallest amount of data needed to distinguish InterMine objects
   from different sources. For example:"
  {:source :flymine-beta
   :type :query
   :value {:from "Gene" :select ["Gene.id"] :where [{:path "Gene.id" :op "=" :value 123}]}})

(def im-package (s/keys :req-un [:package/source
                                 :package/type
                                 :package/value]))