(ns bluegenes.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::non-empty-string (s/and string? (complement str/blank?)))
(s/def ::integer-string (s/and string? #(re-matches #"^[0-9]+$" %)))

(s/def ::server-port (s/or :int int? :string ::integer-string))
(s/def ::logging-level #{"trace" "debug" "info" "warn" "error" "fatal" "report"})
(s/def ::google-analytics (s/or :nil nil? :string string?))
(s/def ::hide-registry-mines boolean?)

(s/def ::bluegenes-tool-path ::non-empty-string)
(s/def ::bluegenes-default-service-root ::non-empty-string)
(s/def ::bluegenes-default-mine-name ::non-empty-string)
(s/def ::bluegenes-default-namespace ::non-empty-string)

(s/def ::root ::non-empty-string)
(s/def ::name ::non-empty-string)
(s/def ::namespace ::non-empty-string)
(s/def ::additional-mine (s/keys :req-un [::root ::name ::namespace]))
(s/def ::bluegenes-additional-mines (s/coll-of ::additional-mine :kind vector?))

(s/def ::bluegenes-config (s/keys :req-un [::bluegenes-tool-path
                                           ::bluegenes-default-service-root ::bluegenes-default-mine-name ::bluegenes-default-namespace]
                                  :opt-un [::bluegenes-additional-mines
                                           ::server-port ::logging-level ::google-analytics ::hide-registry-mines]))

(defn validate-config [env]
  (when-not (s/valid? ::bluegenes-config env)
    (throw (AssertionError. (str "Invalid config: " (s/explain-str ::bluegenes-config env))))))
