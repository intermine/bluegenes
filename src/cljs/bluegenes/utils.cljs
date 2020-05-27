(ns bluegenes.utils
  (:require [clojure.string :as string]
            [clojure.data.xml :as xml]
            [imcljs.query :as im-query]))

(defn uncamel
  "Uncamel case a string. Example: thisIsAString -> This is a string"
  [s]
  (if-not (string/blank? s)
    (as-> s $
      (string/split $ #"(?=[A-Z][^A-Z])")
      (string/join " " $)
      (string/capitalize $))
    s))

(defn read-origin
  "Read the origin class from a query, and infer it if it's missing."
  [query]
  (if-let [origin (:from query)]
    origin
    (first (string/split (first (:select query)) #"\."))))

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

(defn suitable-config?
  "Verifies a tool/viz `config` against a `model` and `entity`, returning
  whether this tool/viz is suitable for displaying."
  [model entity config]
  (when-let [{:keys [format class]} entity]
    (when-let [{:keys [accepts classes depends]} config]
      (and (contains? (set accepts) format)
           (contains? (set classes) class)
           (every? #(contains? model %) (map keyword depends))))))
