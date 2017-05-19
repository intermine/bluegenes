(ns bluegenes.specs
  (:require [clojure.spec :as s]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(s/def :package/type (s/and keyword? (partial one-of? [:string :query])))
(s/def :package/value some?)
(s/def :package/source keyword?)

(def im-package (s/keys :req-un [:package/source
                                 :package/type
                                 :package/value]))


;{:source :flymine
; :type   :query
; :value  {:from "Gene"
;          :select "Gene.id"
;          :where [{:path "Gene"
;                   :op "="
;                   :value "Dad Gene"}]}}
;
;{:source :flymine
; :type   :string
; :value  "mad"}