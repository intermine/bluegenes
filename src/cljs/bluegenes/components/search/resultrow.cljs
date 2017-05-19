(ns bluegenes.components.search.resultrow
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [reagent.core :as reagent]
            [oops.core :refer [ocall oget oget+]]))

(defn is-selected? [result selected-result]
  "returns true if 'result' is selected"
  (= selected-result result))

(defn result-selection-control [result]
  "UI control suggesting to the user that there is only one result selectable at any one time; there's no actual form functionality here."
  (let [selected? (subscribe [:search/am-i-selected? result])]
    [:input {:type "checkbox"
           :on-click (fn [e] (ocall e :stopPropagation)
            (if @selected?
              (dispatch [:search/deselect-result result])
              (dispatch [:search/select-result result])))
           :name "keyword-search"}
   ]))

(defn set-selected! [row-data elem]
  "selects an item and navigates there. "
  (let [current-mine (subscribe [:current-mine])]
    (navigate! (str "/reportpage/" (name (:id @current-mine)) "/" (oget (:result row-data) "type") "/" (oget (:result row-data) "id")))))

(defn row-structure [row-data contents]
  "This method abstracts away most of the common components for all the result-row baby methods."
  (fn [row-data contents]
  (let [result (:result row-data)
        active-filter (subscribe [:search/active-filter])
        selected? (subscribe [:search/am-i-selected? (oget result :id)])]
    ;;Todo: Add a conditional row highlight on selected rows.
    [:div.result {:on-click
                  (fn [this]
                    (set-selected! row-data (oget this "target")))
                  :class (if @selected? "selected")}
     (cond (some? @active-filter)
    [result-selection-control result])
     [:span.result-type {:class (str "type-" (oget result "type"))} (oget result "type")]
     (contents)])))

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
  (let [row (oget (:result row-data) "fields")
        string (get (js->clj row) selector)
        term (:search-term row-data)
        highlight? (re-frame/subscribe [:search/highlight?])]
    (if (and @highlight? string)
      (let [pattern (re-pattern (str "(?i)([\\S\\s]*)" term "([\\S\\s]*)"))
            broken-string (re-seq pattern string)]
        (if broken-string
          (wrap-term (rest (first broken-string)) term)
          [:span string]))
      [:span string])))

(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types and has a default that just outputs all non id, type, and relevance fields."
  (fn [row-data] (oget (:result row-data) "type")))

(defmethod result-row "Gene" [row-data]
  [row-structure row-data
   (fn []
     [:div.details [:ul
                    [:li [:h6 "Organism"] [:span.organism (show row-data "organism.name")]]
                    [:li [:h6 " Symbol: "] (show row-data "symbol")]
                    (let [fields (get-in (js->clj row-data :keywordize-keys true) [:result :fields])
                          primary  (:primaryIdentifier fields)
                          secondary (:secondaryIdentifier fields)]
                      [:li.ids [:h6 " Identifiers: "]
                       (cond primary
                             (show row-data "primaryIdentifier"))

                       (cond (and secondary primary) ", ")
                       (cond secondary (show row-data "secondaryIdentifier"))])]])])

(defmethod result-row "Protein" [row-data]
  [row-structure row-data
   (fn []
     [:div.details
      [:ul
       [:li [:h6 "Organism:"] [:span.organism (show row-data "organism.name")]]
       [:li [:h6 " Accession: "] (show row-data "primaryAccession")]
       [:li.ids [:h6 " Identifiers: "] (show row-data "primaryIdentifier")]]])])

(defmethod result-row "Publication" [row-data]
  [row-structure row-data
   (fn []
     [:div.details
      [:ul
       [:li [:h6 "Author: "] (show row-data "firstAuthor")]
       [:li [:h6 "Title:"] [:cite.publicationtitle " \"" (show row-data "title") "\""]]
       [:li [:h6 "Journal: "] [:div (show row-data "journal") " pp. " (show row-data "pages")]]]])])

(defmethod result-row "Author" [row-data]
  [row-structure row-data
   (fn []
     [:div.details
      (show row-data "name")])])

(defmethod result-row :default [row-data]
  "format a row in a readable way when no other templates apply. Adds 'name: description' default first rows if present."
  (let [details (:fields (js->clj (:result row-data) :keywordize-keys true))]
    [row-structure row-data
     (fn []
       [:div.details
        (if (contains? details "name")
          [:span.name (show row-data "name")])
        (if (contains? details "description")
          [:span.description (show row-data "description")])

        (doall           (reduce (fn [my-list [k value]]
                                   (if (and (not= k "name") (not= k "description"))
                                     (conj my-list [:li [:h6.default-description k] [:div.default-value value]]))) [:ul] details))
])]))
