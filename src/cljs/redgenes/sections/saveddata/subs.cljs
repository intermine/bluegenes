(ns redgenes.sections.saveddata.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :saved-data/all
  (fn [db]
    (sort-by (fn [{created :created}] created) > (vals (get-in db [:saved-data :items])))))

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
  :saved-data/editor-items
  :<- [:saved-data/all]
  :<- [:saved-data/editable-ids]
  (fn [[all editable-ids]]
    (let [ids (map first editable-ids)]
      (->> all
           ; Only show datums that have been selected for editing
           (filter (fn [{id :id}] (some? (some #{id} ids))))

           ; Adjust their queries so only have a path to their database IDs
           (map (fn [item]
                  (assoc-in item [:value :select]
                            (get-in editable-ids [(:id item) :path]))))))))

(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:label details)]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(defn saved-data-has-type? [type {parts :parts}]
  (contains? parts type))

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