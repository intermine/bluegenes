(ns redgenes.sections.saveddata.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :saved-data/all
  (fn [db]
    (sort-by (fn [[_ {created :created}]] created) > (get-in db [:saved-data :items]))))


(reg-sub
  :saved-data/edit-mode
  (fn [db]
    (get-in db [:saved-data :list-operations-enabled])))

(reg-sub
  :saved-data/section
  (fn [db]
    (:saved-data db)))

(reg-sub
  :saved-data/editable-ids
  (fn [db]
    (get-in db [:saved-data :editor :items])))

(reg-sub
  :saved-data/type-filter
  (fn [db]
    (get-in db [:saved-data :editor :filter])))

(reg-sub
  :saved-data/text-filter
  (fn [db]
    (get-in db [:saved-data :editor :text-filter])))

(reg-sub
  :saved-data/editable-items
  :<- [:saved-data/editable-ids]
  :<- [:saved-data/all]
  (fn [[ids items]]
    (filter (fn [[id]]
              (some? (some #{id} ids))) items)))

(defn saved-data-has-type? [type [_ {parts :parts}]]
  (contains? parts type))

(reg-sub
  :saved-data/editor-items
  :<- [:saved-data/all]
  :<- [:saved-data/editable-ids]
  (fn [[all editable-ids]]
    (let [ids (map first editable-ids)]
      (map second (filter (fn [[id]] (some? (some #{id} ids))) all)))))

(defn has-text?
  "Return true if a template's description contains a string"
  [string [_ details]]
  (if string
    (if-let [description (:label details)]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(reg-sub
  :saved-data/filtered-items
  :<- [:saved-data/all]
  :<- [:saved-data/edit-mode]
  :<- [:saved-data/type-filter]
  :<- [:saved-data/text-filter]
  :<- [:saved-data/editable-ids]
  (fn [[all edit-mode type-filter text-filter]]
    (cond->> all
             (and edit-mode type-filter) (filter (partial saved-data-has-type? type-filter))
             text-filter (filter (partial has-text? text-filter)))))