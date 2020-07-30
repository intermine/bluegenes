(ns bluegenes.components.select-tags
  (:require [oops.core :refer [oget]]))

(defn valid-tag?
  "Whether a tag is valid. Full stops and colons are technically permitted but
  we use them for nesting and internal tags respectively."
  [tag-string]
  (re-matches #"[A-Za-z0-9 \-]+" tag-string))

(def invalid-tag-message
  "Tags may only contain letters, numbers, spaces and hyphens.")

(defn main [& {:keys [placeholder on-change value options id]}]
  [:> js/Select.Creatable
   {:placeholder (or placeholder "")
    :inputId id
    :isMulti true
    :isValidNewOption #(valid-tag? %1)
    :formatCreateLabel #(str "Create new \"" % "\" tag")
    :noOptionsMessage #(if ((some-fn empty? valid-tag?) (oget % :inputValue))
                         "No existing tags"
                         invalid-tag-message)
    :onChange (fn [values]
                (->> (js->clj values :keywordize-keys true)
                     (map :value)
                     (not-empty)
                     (on-change)))
    :value (map (fn [v] {:value v :label v}) value)
    :options (map (fn [v] {:value v :label v}) options)}])
