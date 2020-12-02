(ns bluegenes.components.select-tags
  (:require [re-frame.core :refer [subscribe]]
            [oops.core :refer [oget]]
            [bluegenes.subs.auth :as auth]
            [bluegenes.components.bootstrap :refer [poppable]]))

(defn valid-tag?
  "Whether a tag is valid. Full stops and colons are technically permitted but
  we use them for nesting and internal tags respectively. However, superusers
  are allowed colons in tag names so they can add internal tags."
  [superuser? tag-string]
  (if superuser?
    (re-matches #"[A-Za-z0-9 \-:]+" tag-string)
    (re-matches #"[A-Za-z0-9 \-]+" tag-string)))

(def invalid-tag-message
  "Tags may only contain letters, numbers, spaces and hyphens.")

(def invalid-tag-message-superuser
  "Tags may only contain letters, numbers, spaces, colons and hyphens.")

(defn main [& {:keys [placeholder on-change value options id disabled disabled-tooltip]}]
  (let [superuser? @(subscribe [::auth/superuser?])
        select [:> js/Select.Creatable
                {:placeholder (or placeholder "")
                 :inputId id
                 :isMulti true
                 :isValidNewOption #(valid-tag? superuser? %1)
                 :formatCreateLabel #(str "Create new \"" % "\" tag")
                 :noOptionsMessage #(if ((some-fn empty? (partial valid-tag? superuser?))
                                         (oget % :inputValue))
                                      "No existing tags"
                                      (if superuser?
                                        invalid-tag-message-superuser
                                        invalid-tag-message))
                 :onChange (fn [values]
                             (->> (js->clj values :keywordize-keys true)
                                  (map :value)
                                  (not-empty)
                                  (on-change)))
                 :value (map (fn [v] {:value v :label v}) value)
                 :options (map (fn [v] {:value v :label v}) options)
                 :isDisabled disabled}]]
    (if (and disabled (seq disabled-tooltip))
      [poppable {:data disabled-tooltip
                 :children select}]
      select)))
