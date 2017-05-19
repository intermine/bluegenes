(ns bluegenes.sections.saveddata.subs
  (:require-macros [reagent.ratom :refer [reaction]]
                   [com.rpl.specter :refer [traverse select transform]])
  (:require [re-frame.core :refer [reg-sub]]
            [com.rpl.specter :as s]
            [cljs-time.core :as t]))


(defn list->sd
  [list]
  {(:name list) {:sd/created (t/now)
                 :sd/updated (t/now)
                 :sd/count   (:size list)
                 :sd/id      (:name list)
                 :sd/type    :list
                 :sd/label   (:title list)
                 :sd/value   list}})

(reg-sub
  :saved-data/all
  (fn [db]
    (sort-by (fn [{created :sd/created}] created) > (vals (get-in db [:saved-data :items])))))

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
    (get-in db [:saved-data :editor :selected-items])))

(reg-sub
  :saved-data/editable-id
  (fn [db [_ id {path :path}]]
    (first (filter
             (fn [item]
               (= {:id id :path path} item))
             (map #(select-keys % [:id :path])
                  (get-in db [:saved-data :editor :selected-items]))))))

(reg-sub
  :saved-data/type-filter
  (fn [db]
    (get-in db [:saved-data :editor :filter])))

(reg-sub
  :saved-data/list-filter
  (fn [db]
    (get-in db [:saved-data :editor :list-filter])))

(reg-sub
  :saved-data/text-filter
  (fn [db]
    (get-in db [:saved-data :editor :text-filter])))

(reg-sub
  :saved-data/editor-items
  :<- [:saved-data/all]
  :<- [:saved-data/editable-ids]
  (fn [[all editable-ids]]
    (let [ids (map :sd/id editable-ids)]
      (reduce (fn [total next]
                (let [found    (first (filter #(= next (:sd/id %)) all))
                      selected (first (filter #(= next (:sd/id %)) editable-ids))]
                  (conj total
                        (assoc found :selected (select-keys selected [:path :sd/type]))))) [] ids))))


(defn has-text?
  "Return true if a label contains a string"
  [string details]
  (if string
    (if-let [description (:sd/label details)]
      (re-find (re-pattern (str "(?i)" string)) description)
      false)
    true))

(defn saved-data-has-type? [type {parts :sd/parts}]
  (contains? parts type))

(reg-sub
  :saved-data/merge-intersection
  (fn [db]
    (let [items (get-in db [:saved-data :editor :selected-items])]
      (some? (some true? (select [s/ALL :keep :intersection] items))))))


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
