(ns redgenes.components.search.resultrow
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [accountant.core :refer [navigate!]]
            [reagent.core :as reagent]))

(defn is-selected? [result selected-result]
  "returns true if 'result' is selected"
  (= selected-result result))

(defn result-selection-control [result]
  "UI control suggesting to the user that there is only one result selectable at any one time; there's no actual form functionality here."
  [ :input {
      :type "radio"
      :name "keyword-search" ;;todo, dynamic names. would we ever really have two keyword searches on one page though? That seems like madness!
            }])

(defn set-selected! [row-data elem]
  "selects an item and navigates there. "
  (let [current-mine (subscribe [:current-mine])]

    (navigate! (str "#/objects/" (name (:id @current-mine)) "/" (aget (:result row-data) "type") "/" (aget (:result row-data) "id")))
  ))


(defn row-structure [row-data contents]
  "This method abstracts away most of the common components for all the result-row baby methods."
  (let [result (:result row-data)]
  [:div.result {
    :on-click (fn [this] (set-selected! row-data (.-target this)))
    ;:class (if (is-selected? result (:selected-result @state)) "selected")
    }
    [result-selection-control result]
    [:span.result-type {:class (str "type-" (.-type result))} (.-type result)]
    (contents)]
  ))

(defn wrap-term [broken-string term]
  "Joins an array of terms which have already been broken on [term] adding a highlight class as we go.
  So given the search term 'bob' and the string 'I love bob the builder', we'll return something like
  '[:span I love [:span.searchterm 'bob'] the builder]'.
  TODO: If we know ways to refactor this, let's do so. It's verrry slow."
  [:span
    ;; iterate over the string arrays, and wrap span.searchterm around the terms.
    ;; don't do it for the last string, otherwise we end up with random extra
    ;; search terms appended where there should be none.
    (map (fn [string]
        ^{:key string}
        [:span string [:span.searchterm term]]) (butlast broken-string))
      (cond   ;;special case: if both strings are empty, the entire string was the term in question
        (and  ;;so we need to wrap it in searchterm and return the term
          (clojure.string/blank? (last broken-string))
          (clojure.string/blank? (first broken-string)))
        [:span.searchterm term])
      ;;finally, we need to output the last term, without appending anything to it.
      [:span (last broken-string)]])


(defn show [row-data selector]
  "Helper: fetch a result from the data model, adding a highlight if the setting is enabled."
  (let [string (aget (:result row-data) "fields" selector)
        term (:search-term row-data)
        highlight? (re-frame/subscribe [:search/highlight?])]
    (if (and @highlight? string)
      (let [pattern (re-pattern (str "(?i)([\\S\\s]*)" term "([\\S\\s]*)"))
            broken-string (re-seq pattern string)]
        (if broken-string
          (wrap-term (rest (first broken-string)) term)
          [:span string]))
    [:span string]
  )))

(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types and has a default that just outputs all non id, type, and relevance fields."
  (fn [row-data] (.-type (:result row-data))))

(defmethod result-row "Gene" [row-data]
    [row-structure row-data (fn []
      [:div.details
        [:span.organism (show row-data "organism.name")]
        [:span " Symbol: " (show row-data "symbol") ]
        [:span.ids " Identifiers: " (show row-data "primaryIdentifier") ", " (show row-data "secondaryIdentifier")]])])

(defmethod result-row "Protein" [row-data]
   [row-structure row-data (fn []
     [:div.details
        [:span.organism (show row-data "organism.name")]
        [:span " Accession: " (show row-data "primaryAccession") ]
        [:span.ids " Identifiers: " (show row-data "primaryIdentifier")]])])

(defmethod result-row "Publication" [row-data]
  [row-structure row-data (fn []
    [:div.details
      [:span "Author: " (show row-data "firstAuthor")]
      [:cite " \"" (show row-data "title") "\""]
      [:span.journal " (" (show row-data "journal") " pp. " (show row-data "pages")] ")"])])

(defmethod result-row "Author" [row-data]
  [row-structure row-data (fn []
    [:div.details
      (show row-data "name")])])

(defmethod result-row :default [row-data]
  "format a row in a readable way when no other templates apply. Adds 'name: description' default first rows if present."
  (let [details (js->clj (.-fields (:result row-data)))]
  [row-structure row-data (fn []
    [:div.details
    (if (contains? details "name")
      [:span.name (show row-data "name")])
    (if (contains? details "description")
      [:span.description (show row-data "description")])
     (for [[k value] details]
       (if (and (not= k "name") (not= k "description"))
       ^{:key k}
       [:span [:span.default-description k] [:span.default-value value]]))
  ])]))
