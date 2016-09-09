(ns redgenes.sections.saveddata.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter.macros :refer [traverse select transform]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as im-filters]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as t]
            [imcljs.operations :as operations]
            [com.rpl.specter :as s]))


(defn get-parts
  [model query]
  (group-by :type
            (distinct
              (map (fn [path]
                     (assoc {}
                       :type (im-filters/im-type model path)
                       :path (str (im-filters/trim-path-to-class model path) ".id")))
                   (:select query)))))


(reg-event-fx
  :saved-data/toggle-edit-mode
  (fn [{db :db}]
    {:db (update-in db [:saved-data :list-operations-enabled] not)}))

(reg-event-fx
  :save-data
  (fn [{db :db} [_ data]]
    (let [new-id (str (uuid/make-random-uuid))
          model  (get-in db [:assets :model])]
      {:db       (assoc-in db [:saved-data :items new-id]
                           (-> data
                               (merge {:created (t/now)
                                       :updated (t/now)
                                       :parts   (get-parts model (:value data))
                                       :id      new-id})))
       :dispatch [:open-saved-data-tooltip
                  {:label (:label data)
                   :id    new-id}]})))

(reg-event-db
  :open-saved-data-tooltip
  (fn [db [_ data]]
    (assoc-in db [:tooltip :saved-data] data)))

(reg-event-db
  :save-saved-data-tooltip
  (fn [db [_ id label]]
    (-> db
        (assoc-in [:saved-data :items id :label] label)
        (assoc-in [:tooltip :saved-data] nil))))

(reg-event-db
  :saved-data/set-type-filter
  (fn [db [_ kw]]
    (let [clear? (> 1 (count (remove nil? (get-in db [:saved-data :editor :selected-items]))))]
      (if clear?
        (update-in db [:saved-data :editor] dissoc :filter)
        (assoc-in db [:saved-data :editor :filter] kw)))))

(defn index-of [e coll & [keys-to-match]]
  (first (keep-indexed #(if (= (if keys-to-match (select-keys e keys-to-match) e)
                               (if keys-to-match (select-keys %2 keys-to-match) %2)) %1) coll)))

(reg-event-db
  :saved-data/toggle-editable-item
  (fn [db [_ id path-info]]
    (let [loc               [:saved-data :editor :selected-items]
          keys-to-match     [:id :path :type]
          datum-description (merge {:id id} path-info)]
      (if-let [idx (index-of datum-description (get-in db loc) keys-to-match)]
        (assoc-in db (conj loc idx) nil)
        (if-let [first-nil (index-of nil (get-in db loc))]
          (assoc-in db (conj loc first-nil) datum-description)
          (update-in db loc (fnil conj []) datum-description))))))

(reg-event-db
  :saved-data/perform-operation
  (fn [db]
    (let [selected-items (first (get-in db [:saved-data :editor :items]))]
      (let [i (seq selected-items)]
        (println "I" i))
      (let [q1 (assoc
                 (get-in db [:saved-data :items (first selected-items) :value])
                 :select [(:path (second selected-items))]
                 :orderBy nil)]
        (.log js/console q1)
        #_(go (println "done" (<! (operations/operation
                                    {:root "www.flymine.org/query"}
                                    q1 q1)))))
      db)))

(reg-event-db
  :saved-data/set-text-filter
  (fn [db [_ value]]
    (if (= value "")
      (update-in db [:saved-data :editor] dissoc :text-filter)
      (assoc-in db [:saved-data :editor :text-filter] value))))

(reg-event-db
  :saved-data/toggle-keep
  (fn [db [_ id]]
    (let [loc [:saved-data :editor :selected-items]]
      (if id
        (update-in db loc
                  (partial mapv
                           (fn [item] (if (= id (:id item))
                                        (update-in item [:keep :self] not)
                                        item))))
        db))))

(reg-event-db
  :saved-data/toggle-keep-intersections
  (fn [db]
    (update-in db [:saved-data :editor :selected-items]
               (fn [items]
                 (let [selected? (some? (some true? (select [s/ALL :keep :intersection] items)))]
                   (transform [s/ALL]
                              (fn [item]
                                (assoc-in item [:keep :intersection] (not selected?))) items))))))





