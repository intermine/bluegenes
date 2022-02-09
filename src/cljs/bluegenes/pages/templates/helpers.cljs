(ns bluegenes.pages.templates.helpers
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.utils :refer [template-contains-string?]]
            [clojure.string :as str]))

(defn template-matches? [{:keys [category text authorized]} template]
  (let [{tmpl-tags :tags tmpl-authorized :authorized} template]
    (and (if category
           (contains? (set tmpl-tags) (str "im:aspect:" category))
           true)
         (if authorized
           tmpl-authorized
           true)
         (template-contains-string? text [nil template]))))

(defn categories-from-tags [tags]
  (->> tags
       (filter #(str/starts-with? % "im:aspect:"))
       (keep #(-> (re-matches #"im:aspect:(.*)" %) second not-empty))))

(defn categories [templates]
  (into []
        (comp (mapcat (comp categories-from-tags :tags val))
              (distinct))
        templates))

;; Generate web service URL for a template query.

(def op-symbol->text
  {"=" "eq"
   "!=" "ne"
   "<" "lt"
   "<=" "le"
   ">" "gt"
   ">=" "ge"})

(defn ?encode
  "Only encodes the string if it's non-nil. This is because
  js/encodeURIComponent would return 'null' for nil."
  [?s]
  (some-> ?s js/encodeURIComponent))

(defn const->query-string [index {:keys [path op value values extraValue] :as _const}]
  (let [index (inc index)] ; We start counting from 1.
    (-> (str "&constraint" index "=" (?encode path)
             "&op" index "=" (?encode (op-symbol->text op op))
             (let [v (or value values)
                   as-value #(str "&value" index "=" (?encode %))]
               (if (coll? v)
                 (apply str (map as-value v))
                 (as-value v)))
             (when (= op "LOOKUP")
               (str "&extra" index "=" (?encode extraValue))))
        (str/replace "%20" "+"))))

(defn web-service-url [{:keys [root] :as _service} {:keys [name where] :as _template-query}]
  (str root
       "/service/template/results"
       "?name=" (?encode name)
       ;; The constraints get matched based on their path.
       ;; I tested that ordering doesn't matter.
       (apply str (->> where
                       (filter #(and (:editable %)
                                     (not= "OFF" (:switched %))))
                       (map-indexed const->query-string)))
       "&format=tab&size=10"))

;; Preparing template for querying.

(def not-disabled-predicate (comp (partial not= "OFF") :switched))

(defn remove-switchedoff-constraints
  "Filter the constraints of a query map and only keep those with a :switched value other than OFF"
  [query]
  (update query :where #(filterv not-disabled-predicate %)))

(defn clean-template-constraints
  [query]
  (update query :where
          (partial mapv (fn [const]
                          ; :description
                          (dissoc const :editable :switchable :switched :description)))))

(def prepare-template-query
  (comp clean-template-constraints remove-switchedoff-constraints))
