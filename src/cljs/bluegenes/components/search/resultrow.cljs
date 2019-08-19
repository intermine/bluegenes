(ns bluegenes.components.search.resultrow
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [oops.core :refer [ocall oget]]
            [bluegenes.route :as route]))

(defn result-selection-control
  "UI control suggesting to the user that there is only one result selectable at any one time; there's no actual form functionality here."
  [result]
  (let [selected? @(subscribe [:search/am-i-selected? result])]
    [:input {:type "checkbox"
             :on-change (fn [e]
                          (if (oget e :target :checked)
                            (dispatch [:search/select-result result])
                            (dispatch [:search/deselect-result result])))
             ;; on-change is the React idiomatic way to handle interaction, but
             ;; for some reason calling stopPropagation in there doesn't work!
             :on-click #(ocall % :stopPropagation)
             :checked selected?
             :name "keyword-search"}]))

(defn row-structure
  "This method abstracts away most of the common components for all the result-row baby methods."
  [row-data contents]
  (let [{:keys [id type] :as result} (:result row-data)
        category-filter? (subscribe [:search/category-filter?])
        selected?        (subscribe [:search/am-i-selected? id])]
    ;;Todo: Add a conditional row highlight on selected rows.
    [:div.result
     {:on-click #(dispatch [::route/navigate ::route/report {:type type :id id}])
      :class (if @selected? "selected")}
     (when @category-filter?
       [result-selection-control result])
     [:span.result-type {:class (str "type-" type)} type]
     (contents)]))

(defn wrap-term
  "Joins an array of terms which have already been broken on [term] adding a highlight class as we go.
  So given the search term 'bob' and the string 'I love bob the builder', we'll return something like
  '[:span I love [:span.searchterm 'bob'] the builder]'.
  TODO: If we know ways to refactor this, let's do so. It's verrry slow."
  [broken-string term]
  [:span
   ;; iterate over the string arrays, and wrap span.searchterm around the terms.
   ;; don't do it for the last string, otherwise we end up with random extra
   ;; search terms appended where there should be none.
   (map (fn [string]
          ^{:key string}
          [:span string [:span.searchterm term]]) (butlast broken-string))
   (cond ;;special case: if both strings are empty, the entire string was the term in question
     (and ;;so we need to wrap it in searchterm and return the term
      (clojure.string/blank? (last broken-string))
      (clojure.string/blank? (first broken-string)))
     [:span.searchterm term])
   ;;finally, we need to output the last term, without appending anything to it.
   [:span (last broken-string)]])

(defn show
  "Helper: fetch a result from the data model, adding a highlight if the setting is enabled."
  [row-data selector]
  (let [row        (:fields (:result row-data))
        string     (get row (keyword selector))
        term       (:search-term row-data)
        highlight? (re-frame/subscribe [:search/highlight?])]
    (if (and @highlight? string)
      (let [pattern       (re-pattern (str "(?i)([\\S\\s]*)" term "([\\S\\s]*)"))
            broken-string (re-seq pattern string)]
        (if broken-string
          (wrap-term (rest (first broken-string)) term)
          [:span string]))
      [:span string])))

(defmulti result-row
  "Result-row outputs nicely formatted type-specific results for common types
  and has a default that just outputs all non id, type, and relevance fields."
  (fn [row-data] (:type (:result row-data))))

(defmethod result-row "Gene" [row-data]
  [row-structure row-data
   (fn []
     [:div.details [:ul
                    [:li [:h6 "Organism"] [:span.organism (show row-data "organism.name")]]
                    [:li [:h6 " Symbol: "] (show row-data "symbol")]
                    (let [fields    (get-in row-data [:result :fields])
                          primary   (:primaryIdentifier fields)
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

;; format a row in a readable way when no other templates apply.
;; Adds 'name: description' default first rows if present.
(defmethod result-row :default [row-data]
  (let [details (:fields (:result row-data))]
    [row-structure row-data
     (fn []
       [:div.details
        (when (contains? details :name)
          [:span.name (show row-data "name")])
        (when (contains? details :description)
          [:span.description (show row-data "description")])
        (into [:ul]
              (comp (filter (comp (complement #{:name :description}) key))
                    (map (fn [[k v]]
                           [:li [:h6.default-description k] [:div.default-value v]])))
              details)])]))
