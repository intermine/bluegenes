(ns bluegenes.titles
  (:require [clojure.string :as string]
            [bluegenes.pages.reportpage.components.summary :as summary]))

(def document-titles
  "Define document title templates for each corresponding panel keyword.
  This should be a map with panel keyword keys and vector values. The vector
  will be joined and should consist of db pointers (explained below)."
  (let [App    "InterMine BlueGenes"
        Mine   #(get-in % [:mines (:current-mine %) :name])
        Type   [:report :summary :rootClass]
        Name   #(summary/choose-title-column (get-in % [:report :summary]))
        Debug  [:debug-panel]
        Search [:search-results :keyword]
        Query  [:results :history-index]]
    {:home-panel         ["Home"             Mine App]
     :profile-panel      ["Profile"          Mine App]
     :debug-panel        [Debug "Debug"      Mine App]
     :templates-panel    ["Templates"        Mine App]
     :reportpage-panel   [Name Type "Report" Mine App]
     :upload-panel       ["Upload"           Mine App]
     :search-panel       [Search "Search"    Mine App]
     :results-panel      [Query "Results"    Mine App]
     :regions-panel      ["Region Search"    Mine App]
     :lists-panel        ["Lists"            Mine App]
     :querybuilder-panel ["Query Builder"    Mine App]}))

(defn *db->str
  "Converts a db pointer to its value, usually a string.
  A db pointer can be one of the following types:
      function - called with db as argument
      vector   - called as third argument to `(get-in db)`
      string   - passed through"
  [db *db]
  (condp (fn [test-fn value] (test-fn value)) *db
    fn?     (*db db)
    vector? (get-in db *db)
    *db))

(defn db->title
  "Takes an app-db as argument and returns the corresponding title."
  [db]
  (->> (:active-panel db)
       document-titles
       (map (partial *db->str db))
       (filter some?)
       (string/join " - ")))
