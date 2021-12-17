(ns bluegenes.utils
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [clojure.walk :as walk]
            [imcljs.query :as im-query]
            [bluegenes.version :as version]
            [bluegenes.components.icons :refer [icon]]
            [markdown-to-hiccup.core :as md]
            [goog.string :as gstring]
            [oops.core :refer [ocall]]
            [clojure.data.xml :as xml]
            [bluegenes.config :refer [server-vars]]))

(defn hiccup-anchors-newtab
  "Add target=_blank to all anchor elements, so all links open in new tabs."
  [hiccup]
  (walk/postwalk (fn [e] (if (and (map? e) (contains? e :href))
                           (assoc e :target "_blank")
                           e))
                 hiccup))

(defn md-paragraph
  "Returns the `[:p]` hiccup for a specified markdown string paragraph.
  Usage:
      [:div (md-paragraph \"Foo *bar* [baz](http://baz.com)\")]
  Note that only the first paragraph in the markdown string will be parsed;
  any other elements before or after will be ignored, and so will any proceeding
  paragraphs."
  [md-string]
  (->> (some-> md-string md/md->hiccup md/component (md/hiccup-in :div :p))
       (hiccup-anchors-newtab)))

(defn md-element
  "Returns hiccup for a specified markdown string, with a containing div."
  [md-string]
  (->> (some-> md-string md/md->hiccup md/component)
       (hiccup-anchors-newtab)))

;; Please do not use this for model classes. You can get results like:
;;     "ThreePrimeUTR" -> "Three primeutr"
;;     "ThisIsADog" -> "This isa dog"
;; I don't think camelcasing is a reversible operation unless you explicitly
;; specify all acronyms present (inflections.core uses this approach).
;; Imagine "ThisIsAUTR": with the perfect implementation it would uncamel to
;; "This is AUTR"; without defining UTR as an acronym, you wouldn't get "This
;; is a UTR".
;; Instead of using this function, use the `displayName` of the class, or just
;; display the camelcased name (it isn't that bad).
(defn uncamel
  "Uncamel case a string. Example: thisIsAString -> This is a string"
  [s]
  (if-not (string/blank? s)
    (as-> s $
      (string/split $ #"(?=[A-Z][^A-Z])")
      (string/join " " $)
      (string/capitalize $))
    s))

;; You may think update-in serves you just fine
;;     (update-in {} [:foo :bar] dissoc :baz)
;; but this ends up creating nested maps all the way until the inner nil.
;; `dissoc-in` on the other hand, won't do this, and will remove the nested
;; maps if they don't hold any other keys than the one you wanted removed.
(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn read-origin
  "Read the origin class from a query, and infer it if it's missing."
  [query]
  (if-let [origin (:from query)]
    origin
    (first (string/split (first (:select query)) #"\."))))

;; There's a reason why `remvec` and `addvec` are not part of Clojure, and
;; that's because they are inefficient operations on the vector type! If the
;; vector won't get very large, it's completely fine to use them. Otherwise
;; you'll be better off with a map, set, perhaps combined with a vector, or
;; external libs like `org.flatland/ordered`.

(defn remvec
  "Remove an element from a vector by index. Will throw if the index does not exist."
  [v i]
  (vec (concat (subvec v 0 i)
               (subvec v (inc i)))))

(defn addvec
  "Add an element to a vector by index. If the index is negative, the element
  be added to the beginning of the vector. If the index is greater than the
  vector's length, it will be added to the end."
  [v i e]
  (let [[before after] (split-at i v)]
    (vec (concat before [e] after))))

(defn kw->str
  [kw]
  (if (keyword? kw)
    (str (namespace kw)
         (when (namespace kw) "/")
         (name kw))
    (do (assert (string? kw) "This function takes only a keyword or string.")
        kw)))

(defn read-registry-mine
  "Grab the most important data from a mine object retrieved from the registry.
  This is how a mine is initially created in `(:mines app-db)`, before it is
  populated with the responses from fetching assets."
  [reg-mine]
  {:service {:root (:url reg-mine)}
   :name (:name reg-mine)
   :id (-> reg-mine :namespace keyword)
   :logo (-> reg-mine :images :logo)})

(defn get-mine-ns
  "Return the mine namespace as a keyword.
  Handles both mines from the registry and config."
  [mine]
  (if (contains? mine :service) ; Only config mines have :service
    (:id mine)
    (keyword (:namespace mine))))

(defn get-mine-url
  "Return the mine url.
  Handles both mines from the registry and config."
  [mine]
  (if (contains? mine :service) ; Only config mines have :service
    (get-in mine [:service :root])
    (:url mine)))

(defn read-xml-query
  "Read an InterMine PathQuery in XML into an EDN Clojure map.
  Will throw on invalid XML."
  [xml-query]
  (let [xml-map         (xml/parse-str xml-query)
        select          (string/split (get-in xml-map [:attrs :view] " ") #" ")
        _               (when (empty? select) (throw (js/Error. "Invalid PathQuery XML")))
        from            (not-empty (first (string/split (first select) #"\.")))
        constraintLogic (get-in xml-map [:attrs :constraintLogic])
        orderBy         (let [{:keys [sortOrder orderBy]} (:attrs xml-map)
                              pairs (partition 2 (string/split (or sortOrder orderBy) #" "))]
                          (mapv (fn [[path dir]]
                                  {(keyword path) (string/upper-case dir)}) pairs))
        joins           (into []
                              (comp (filter (comp #{:join} :tag))
                                    (filter (comp #{"OUTER"} :style :attrs))
                                    (map (comp :path :attrs)))
                              (:content xml-map))
        where           (into []
                              (comp (filter (comp #{:constraint} :tag))
                                    (map (fn [{:keys [attrs content]}]
                                           (cond-> attrs
                                             (not-empty content)
                                             ;; Handle ONE OF constraints.
                                             (assoc :values
                                                    (->> content
                                                         (filter (comp #{:value} :tag))
                                                         (mapcat :content)))))))
                              (:content xml-map))]
    {:from from
     :select select
     :orderBy orderBy
     :constraintLogic constraintLogic
     :joins joins
     :where where}))

(defn suitable-entities
  "Removes key-value pairs from an entities map which don't adhere to config.
  Can also be used to check whether a tool should be displayed, as it will
  return nil if no entity is suitable at all.
  1. Check that the tool's API version matches this Bluegenes
  2. Check that the tool's model dependencies are present
  3. Remove entity pairs which don't match tool's accepted formats
  4. Pick entity pairs whose class match tool's classes
     a. All entity classes match when `*` wildcard is used
     b. Entity classes which are a subclass to a tool's class match
        i. During a subclass match, the class in the entity is replaced with the
           tool's class it matched with (e.g. ORF -> Gene)
        ii. If there are multiple subclasses that match with the same tool's
            class, only the last one is kept (due to conflicting keys)."
  [model hier entities config]
  (when-let [{:keys [accepts classes depends version]
              :or {version 1}} config]
    (when (and (= version version/tool-api)
               (every? #(contains? model %) (map keyword depends)))
      (as-> entities $
        (into {} (filter (comp (set accepts) :format val)) $)
        (if (some #{"*"} classes)
          $
          (into {}
                (keep (fn [[entity-class entity-map :as entity]]
                        (some (fn [class-kw]
                                (cond
                                  (= entity-class class-kw) entity
                                  (isa? hier entity-class class-kw) [class-kw entity-map]))
                              (map keyword classes)))
                      $)))
        (not-empty $)))))

(defn version-string->vec
  "Converts a version string consisting of one or more whole numbers separated
  by non-numeric characters into a vector of integers. Returns nil if the
  version string can't be interpreted."
  [vstring]
  (some->> vstring
           (re-seq #"\d+")
           (mapv #(js/parseInt % 10))))

(defn compatible-version?
  "Returns whether `version` is compatible with `required-version`, meaning it
  must be greater than or equal. Versions can be either a string or vector of
  integers. Will return false if versions have differing amount of subversions."
  [required-version version]
  (let [version (cond-> version
                  (string? version) version-string->vec)
        required-version (cond-> required-version
                           (string? required-version) version-string->vec)]
    (if (= (count version)
           (count required-version))
      (reduce
       (fn [_ [index v]]
         (let [rv (nth required-version index)]
           (cond
             (< v rv) (reduced false)
             (> v rv) (reduced true)
             ;; This assures that `true` is returned if all subversions are equal.
             :else true)))
       nil
       (map-indexed vector version))
      false)))

(defn parse-template-rank [rank]
  (let [rank-num (js/parseInt rank)]
    ;; Template ranks come back as strings, either "unranked", or
    ;; integers that have become stringy, e.g. "12". If we don't parse
    ;; them into ints, the order becomes 1, 11, 12, 2, 23, 25, 3, etc.
    ;; but we also need to handle the genuine strings, which become NaN
    ;; when we try to parse them.
    (if (.isNaN js/Number rank-num)
      ;; unranked == last please.
      ;; I sincerely hope we never have 100k templates
      99999
      ;; if it's a number, just return it.
      rank-num)))

(defn extract-tag-categories [tags]
  (->> tags
       (keep #(second (re-matches #"im:aspect:([^\s]+)" %)))
       (string/join " ")))

(defn template-contains-string? [s [_ template]]
  (if (empty? s)
    true
    (let [ss (map string/lower-case (-> s string/trim (string/split #"\s+")))
          {:keys [name title description comment tags]} template
          all-text (->> (extract-tag-categories tags)
                        (str name " " title " " description " " comment)
                        (string/lower-case))]
      (every? #(string/includes? all-text %) ss))))

(defn ascii-arrows
  "Returns a seq of all arrows present in a template title.
  Useful for checking whether there are any arrows present."
  [s]
  (re-seq #"(?:-+>|<-+)" s))

(defn flatten-seq
  "Works like flatten except it will only remove seqs; keeping vectors, lists
  and other sequential things. This is useful when you have hiccup with seqs
  interwoven and want to clean it up to get a flat sequence of elements."
  [x]
  (filter (complement seq?)
          (rest (tree-seq seq? seq x))))

(defn ascii->svg-arrows
  "Replaces arrows in template titles with prettier svg icons."
  [s & {:keys [max-length]}]
  (flatten-seq
   (interpose [icon "arrow-right"]
              (map (fn [part]
                     (interpose [icon "arrow-left"]
                                (map (fn [subpart]
                                       [:span (if (and (number? max-length) (> (count subpart) max-length))
                                                (str (subs subpart 0 max-length) "...")
                                                subpart)])
                                     (string/split part #"<-+"))))
                   (string/split s #"-+>")))))

(defn clean-tool-name
  "Most tools have a name starting with 'BlueGenes' which is frankly not very
  useful, so we remove it."
  [human-name]
  (string/replace human-name #"(?i)^bluegenes\s*" ""))

(defn highlight-substring
  "Extracts all instances of substring (case-insensitive) in a string as span
  elements with a unique CSS class (defaults to .text-highlight). Will return
  a sequential of span elements, meaning you'll want to use it with into."
  ([s substr]
   (highlight-substring s substr :text-highlight))
  ([s substr css-class]
   (cond
     (empty? s) []
     (empty? substr) [[:span s]]
     :else
     (let [re (re-pattern (str "(?i)" (gstring/regExpEscape substr)))
           fragments (map (fn [s] (if (empty? s) nil [:span s])) (string/split s re))
           excerpts (map (fn [s] [:span {:class css-class} s]) (re-seq re s))
           length (max (count fragments) (count excerpts))
           pad-fragments (- length (count fragments))
           pad-excerpts (- length (count excerpts))]
       (remove nil?
               (interleave (cond-> fragments
                             (pos? pad-fragments) (concat (repeat pad-fragments nil)))
                           (cond-> excerpts
                             (pos? pad-excerpts) (concat (repeat pad-excerpts nil)))))))))

(defn rows->maps
  "Takes an `imcljs.fetch/rows` response and transforms it into a vector of
  maps, with the last portion of the path as keyword keys ('Gene.symbol' -> :symbol)."
  [res]
  (let [views (map (comp keyword #(re-find #"[^\.]+$" %)) (:views res))]
    (mapv (fn [result]
            (zipmap views result))
          (:results res))))

(defn encode-file
  "Encode a stringified text file such that it can be downloaded by the browser.
  Results must be stringified - don't pass objects / vectors / arrays / whatever.
  Ideally for performance, you'll want to invoke
      (ocall js/window.URL :revokeObjectURL url)
  where `url` is what's returned by this function, once you're done using it."
  [data filetype]
  (ocall js/URL "createObjectURL"
         (js/Blob. (clj->js [data])
                   {:type (str "text/" filetype)})))

(defn mine-from-pathname
  "Return mine name using pathname, or nil if not present. Factors in deploy path."
  []
  (let [pathname (.. js/window -location -pathname)
        deploy-path (:bluegenes-deploy-path @server-vars)]
    (-> pathname
        (subs (min (count (str deploy-path "/"))
                   (count pathname)))
        (string/split #"/")
        (first)
        (not-empty))))

;; Example of what a template XML is expected to look like.

; <template name="foo_bar" title="foo --&gt; bar" comment="">
;    <query name="foo_bar" model="genomic" view="Gene.chromosome.primaryIdentifier Gene.chromosomeLocation.strand Gene.chromosomeLocation.start Gene.chromosomeLocation.end Gene.length Gene.symbol Gene.secondaryIdentifier" longDescription="For a specified organism, show the chromosomal location and sequence length for all genes." sortOrder="Gene.chromosome.primaryIdentifier asc">
;       <constraint path="Gene.organism.name" editable="true" switchable="on" op="=" value="Drosophila melanogaster" />
;    </query>
; </template>

(defn template->xml
  "Generate a template XML string, for use with web services that expect a
  template XML, e.g. for saving a template."
  [model {:keys [name title description comment]} query]
  (xml/emit-str
   (xml/element :template {:name name :title title :comment comment}
                (xml/parse-str (im-query/->xml model (assoc query :longDescription description))))))

(defn template-objects->xml
  "Takes a collection of template objects, as returned by the templates web
  service for JSON format, and converts it to XML."
  [model template-objects]
  (xml/emit-str
   (xml/element* :template-queries {}
                 (map (comp xml/parse-str #(template->xml model % %)) template-objects))))

(comment
  "Use this to try out template->xml using nREPL."
  (require '[re-frame.core :refer [subscribe]])
  (template-objects->xml
   @(subscribe [:current-model])
   (map @(subscribe [:templates]) [:Foo_bar :Foo_bar_baz]))
  (template->xml @(subscribe [:current-model])
                 {:name "foo_bar"
                  :title "foo --> bar"
                  :comment ""
                  :description "For a specified organism, show the chromosomal location and sequence length for all genes."}
                 {:from "Gene",
                  :select
                  ["Gene.secondaryIdentifier"
                   "Gene.symbol"
                   "Gene.goAnnotation.ontologyTerm.parents.name"
                   "Gene.goAnnotation.ontologyTerm.parents.identifier"
                   "Gene.goAnnotation.ontologyTerm.name"
                   "Gene.goAnnotation.ontologyTerm.identifier"],
                  :constraintLogic "(A and B)",
                  :where
                  [{:op "=",
                    :code "A",
                    :value "DNA binding",
                    :path "Gene.goAnnotation.ontologyTerm.parents.name"
                    :editable true
                    :switchable "on"}
                   {:op "=",
                    :code "B",
                    :value "Drosophila melanogaster",
                    :path "Gene.organism.name"
                    :editable true}
                   {:path "Gene.goAnnotation.ontologyTerm", :type "GOTerm"}
                   {:path "Gene.goAnnotation.ontologyTerm.parents", :type "GOTerm"}],
                  :sortOrder [{:path "Gene.secondaryIdentifier", :direction "ASC"}],
                  :joins []}))
