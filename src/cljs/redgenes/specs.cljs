(ns redgenes.specs
  (:require [clojure.spec :as s]))

(defn one-of? [haystack needle] (some? (some #{needle} haystack)))

(s/def :package/type (s/and keyword? (partial one-of? [:list :query])))
(s/def :package/contents (s/keys :req-un [:package/type :package/value]))
(s/def :package/source keyword?)

(def im-package (s/keys :req-un [:package/source
                                 :package/contents]))
