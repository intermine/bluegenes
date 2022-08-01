(ns bluegenes.pages.reportpage.utils
  (:require [clojure.string :as str]
            [imcljs.path :as im-path]
            [bluegenes.pages.admin.events :refer [new-category new-child]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.utils :refer [md-element]]
            [bluegenes.components.icons :refer [icon]]))

;; We have a special section that we prepend to all report pages. To keep
;; things consistent and easy to change, we only define its properties below.
(def ^:const pre-section-title "Summary")
(def ^:const pre-section-id "summary")

(defn title-column
  "Find the most suitable column for a title (usually symbol or identifier)."
  [class {:keys [views results] :as _summary}]
  (let [views->values (zipmap views (first results))]
    (some (fn [attrib]
            (some (fn [[view value]]
                    (when (and value
                               ;; The below ensures that the attribute belongs to the class, and not a coll/ref of the class.
                               (= view (str class "." attrib)))
                      value))
                  views->values))
          ["symbol"
           "identifier"
           "secondaryIdentifier"
           "primaryIdentifier"
           "name"
           "title"
           "id"])))

(defn strip-class
  "Removes everything before the first dot in a path, effectively removing the root class.
  (strip-class `Gene.organism.name`)
  => `organism.name`"
  [path-str]
  (second (re-matches #"[^\.]+\.(.*)" path-str)))

;; We don't use the `imcljs.path` functions here as we're manually building a
;; path by appending a tail to a different root.
(defn ->query-ref+coll [summary-fields object-type object-id ref+coll]
  (let [{:keys [name referencedType]} ref+coll]
    {:from object-type
     :select (->> (get summary-fields (keyword referencedType))
                  (map strip-class)
                  (map (partial str object-type "." name ".")))
     :where [{:path (str object-type ".id")
              :op "="
              :value object-id}]}))

(defn runnable-templates
  "Some templates can automatically be run on a report page if they meet certain conditions.
  1. They must have a single editable constraint
  2. That single constraint must be of type LOOKUP
  3. That single constraint must be backed by the same class as the item on the report page"
  [templates model & [class]]
  ; Starting with all templates for the current mine...
  (when (and templates model)
    (->> templates
         ; Only keep ones that meet the following criteria (return something other than nil)
         (filter (fn [[_template-kw {:keys [where]}]]

                   ; When we only have one editable constraint
                   (when (= 1 (count (filter :editable where)))
                     ; Make a list of indices that are LOOKUP, editable, and
                     ; have a backing class of the report page item type

                     (when-let [replaceable-indexes
                                (not-empty (keep-indexed (fn [idx {:keys [path op editable]}]
                                                           (when (and (= op "LOOKUP")
                                                                      ;; class is an optional argument; we won't filter by it
                                                                      ;; if the caller wants templates regardless of class.
                                                                      (if class
                                                                        (= class
                                                                           (name (im-path/class (assoc model :type-constraints where) path)))
                                                                        true)
                                                                      (= editable true))
                                                             idx))
                                                         where))]

                       ; When that list of indices is 1, aka we only have one constraint to change
                       (= 1 (count replaceable-indexes)))))))))

(defn init-template
  "Initialize a runnable template with details of the active report page."
  [current-model object-type object-id {:keys [where] :as template-details}]
  ; Update the index that is LOOKUP and editable, with the report page item id
  ; and change the op to = and also update the path (which ends on a class) to
  ; include ".id" on the end.  Don't make the assumption that it's
  ; <report-item-type>.id as a query's root might not be the same class as the
  ; object on the report page
  (let [replaceable-index (first (keep-indexed (fn [idx {:keys [path op editable :as constraint]}]
                                                 (when (and (= op "LOOKUP")
                                                            (= object-type
                                                               (name (im-path/class (assoc current-model :type-constraints where) path)))
                                                            (= editable true))
                                                   idx))
                                               where))]
    (if (number? replaceable-index)
      (-> template-details
          (update-in [:where replaceable-index :path] str ".id")
          (update-in [:where replaceable-index] assoc
                     :value object-id
                     :op "="))
      template-details)))

(defn fallback-layout
  "Autogenerates a reasonable report page layout when a human hasn't created one."
  [tools classes templates]
  (cond-> []
    (seq tools) (conj (update (new-category "Visualizations")
                              :children into (map #(new-child % :collapse true) tools)))
    (seq classes) (conj (update (new-category "Data")
                                :children into (map new-child classes)))
    (seq templates) (conj (update (new-category "Templates")
                                  :children into (map new-child templates)))))

(defn description-dropdown
  "Dropdown and poppable element for use inside a report item heading to show a
  markdown description."
  [text]
  [:span.dropdown
   [:a.dropdown-toggle
    {:data-toggle "dropdown"
     :role "button"
     :on-click #(.stopPropagation %)}
    [poppable {:data (md-element text)
               :children [icon "info"]}]]
   [:div.dropdown-menu.report-item-description
    [:form {:on-submit #(.preventDefault %)
            :on-click #(.stopPropagation %)}
     (md-element text)]]])
