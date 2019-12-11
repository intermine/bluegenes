(ns bluegenes.pages.reportpage.components.summary
  (:require [re-frame.core :refer [subscribe dispatch]]
            [oops.core :refer [ocall]]
            [bluegenes.pages.reportpage.subs :as subs]))

(defn field []
  (fn [k v]
    [:div.field
     [:div.field-label [:h4 (last (clojure.string/split k " > "))]]
     [:div.field-value
      (cond (nil? v) "N/A"
            :else (str v))]]))

(defn is-entity-identifier?
  "simple string checking method to see if a given path type is appropriate for a report page title"
  [path]
  (or (clojure.string/ends-with? path "dentifier") ; i or I.
      (clojure.string/ends-with? path "symbol")))

(defn choose-title-column
  "Finds the first summary fields column that's an identifier or symbol. First field returned isn't always correct (in beany mines it can be the organism associated with the report page result, and not the identifier itself)."
  [field-map]
  (let [results (first (:results field-map))
        first-column (first (filter some? results))
        ;;build a vector of k/v pairs that could be suitable
        identifier-columns (reduce-kv (fn [new-vec index view]
                                        (if (and (is-entity-identifier? view) (some? (get results index)))
                                          (conj new-vec (get results index))
                                          new-vec)) [] (:views field-map))]
    (first identifier-columns)))

(defn fasta-dropdown []
  (let [fasta @(subscribe [::subs/fasta])]
    [:span.dropdown
     [:a.dropdown-toggle.fasta-button
      {:data-toggle "dropdown" :role "button" :title "Show sequence"}
      "FASTA..."]
     [:div.dropdown-menu.fasta-dropdown
      [:form ; Top secret technique to avoid closing the dropdown when clicking inside.
       [:pre.fasta-sequence fasta]]]]))

(defn encode-file
  "Encode a stringified text file such that it can be downloaded by the browser.
  Results must be stringified - don't pass objects / vectors / arrays / whatever."
  [data filetype]
  (ocall js/URL "createObjectURL"
         (js/Blob. (clj->js [data])
                   {:type (str "text/" filetype)})))

(defn fasta-download []
  (let [id           (subscribe [::subs/fasta-identifier])
        fasta        (subscribe [::subs/fasta])
        download-ref (atom nil)
        download!    #(let [el @download-ref]
                        (ocall el :setAttribute "href" (encode-file @fasta "fasta"))
                        (ocall el :setAttribute "download" (str @id ".fasta"))
                        (ocall el :click))]
    (fn []
      [:<>
       [:a.hidden-download {:download "download" :ref (fn [el] (reset! download-ref el))}]
       [:a.fasta-download {:role "button" :title "Download sequence" :on-click download!}
        "Download"
        [:svg.icon.icon-download [:use {:xlinkHref "#icon-download"}]]]])))

(defn fasta-fields []
  (let [fasta               @(subscribe [::subs/fasta])
        chromosome-location @(subscribe [::subs/chromosome-location])
        fasta-length        @(subscribe [::subs/fasta-length])]
    (when fasta
      [:<>
       [:div.field
        [:div.field-label [:h4 "Chromosome location"]]
        [:div.field-value chromosome-location]]
       [:div.field
        [:div.field-label [:h4 "Sequence length"]] 
        [:div.field-value.fasta-value fasta-length
         [fasta-dropdown]
         [fasta-download]]]])))

(defn main [field-map]
  [:div.report-summary
   [:div
    [:h1 (str (:rootClass field-map) ": " (choose-title-column field-map))]]
   (let [entries (zipmap (:columnHeaders field-map)
                         (first (:results field-map)))]
     (-> [:div.fields]
         (into (map (fn [[f v]] [field f v]) entries))
         (into [[fasta-fields]])))])
