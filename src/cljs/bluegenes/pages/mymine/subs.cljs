(ns bluegenes.pages.mymine.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [bluegenes.pages.mymine.listsubs]))

(reg-sub
 ::details-keys
 (fn [db] (get-in db [:mymine :details])))

(reg-sub
 ::details
 (fn [] [(subscribe [::details-keys]) (subscribe [:lists/filtered-lists])])
 (fn [[{:keys [id file-type]} lists]]
   (first (filter (comp (partial = id) :id) lists))))

(reg-sub
 ::context-menu-target
 (fn [db]
   (get-in db [:mymine :context-menu-target])))

(reg-sub
 ::checked-ids
 (fn [db] (get-in db [:mymine :checked])))

(reg-sub
 ::checked-details
 (fn [] [(subscribe [:lists/filtered-lists])
         (subscribe [::checked-ids])])
 (fn [[lists checked-ids]]
   (filter (fn [l] (some #{(:id l)} checked-ids)) lists)))

(reg-sub
 ::suggested-modal-state
 (fn [db]
   (get-in db [:mymine :suggested-state])))

(reg-sub
 ::one-list
 (fn [db [_ list-id]]
   (let [current-lists (get-in db [:assets :lists (get db :current-mine)])]
     (->> current-lists (filter #(= list-id (:id %))) first))))

(reg-sub
 ::modal
 (fn [db]
   (get-in db [:mymine :modal])))

(reg-sub
 ::menu-details
 (fn [db]
   (get-in db [:mymine :menu-file-details])))

(reg-sub
 ::untagged-items
 :<- [:lists/filtered-lists]
 (fn [lists [evt]]
   (map (fn [l] {:im-obj-type "list" :im-obj-id (:id l)}) lists)))

(reg-sub
 ::dragging
 (fn [db]
   (get-in db [:mymine :drag :dragging])))

(reg-sub
 ::dragging?
 (fn [db]
   (get-in db [:mymine :drag :dragging?])))

(reg-sub
 ::dragging-over
 (fn [db]
   (get-in db [:mymine :drag :dragging-over])))

(reg-sub
 ::dropping-on
 (fn [db]
   (get-in db [:mymine :drag :dropping-on])))

(reg-sub
 ::modal-data
 (fn [db [_ path]]
   (get-in db (into [:mymine :modals] path))))
