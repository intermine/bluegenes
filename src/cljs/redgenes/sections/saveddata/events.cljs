(ns redgenes.sections.saveddata.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [com.rpl.specter :refer [traverse select transform]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as im-filters]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as t]
            [imcljs.operations :as operations]
            [imcljs.search :as search]
            [clojure.set :refer [union intersection difference]]))

(def event-namespace :saved-data)

(defn ekw
  "Creates a fully qualified keyword for events"
  [kw]
  (keyword (name event-namespace) (name kw)))

(defn put-at
  "Sugar function for assoc'ing a value somewhere in app db
  (reg-event-db :store-results (put-at [:assoc-in-here :then-here])"
  [where]
  (fn [db [_ results]]
    (assoc-in db where results)))

(defn update-at
  "Sugar function for updating a value somewhere in app db
  (reg-event-db :update-results (run-at [:assoc-in-here :then-here] not)"
  [where f]
  (fn [db]
    (update-in db where f)))


(defn index-of-map
  "Returns the index of a map in a collection.
  Accepts optional argument for only matching on certain keys"
  [e coll & [keys-to-match]]
  (first (keep-indexed #(if (= (if keys-to-match (select-keys e keys-to-match) e)
                               (if keys-to-match (select-keys %2 keys-to-match) %2)) %1) coll)))


"Pointless rename of reg-event-db"
(def when-event (partial reg-event-db))

(defn get-parts
  "Get all of the different parts of an intermine query and group them by type"
  [model query]
  (group-by :type
            (distinct
              (map (fn [path]
                     (assoc {}
                       :type (im-filters/im-type model path)
                       :path (str (im-filters/trim-path-to-class model path) ".id")))
                   (:select query)))))



(when-event :open-saved-data-tooltip (put-at [:tooltip :saved-data]))
(when-event (ekw :toggle-edit-mode) (update-at [:saved-data :list-operations-enabled] not))
(when-event (ekw :save-operation-results) (put-at [:saved-data :editor :results]))
(when-event (ekw :editor-is-editing) (put-at [:saved-data :editor :editing?]))

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



(reg-event-db
  :saved-data/toggle-editable-item
  (fn [db [_ id path-info]]
    (let [loc               [:saved-data :editor :selected-items]
          keys-to-match     [:id :path :type]
          datum-description (merge {:id id} path-info)]
      (if-let [idx (index-of-map datum-description (get-in db loc) keys-to-match)]
        (assoc-in db (conj loc idx) nil)
        (if-let [first-nil (index-of-map nil (get-in db loc))]
          (assoc-in db (conj loc first-nil) datum-description)
          (update-in db loc (fnil conj []) datum-description))))))


(defn determine-op
  [item-1 item-2]
  (let [keep-1 (:keep item-1)
        keep-2 (:keep item-2)]
    (cond
      (and (:self keep-1) (:self keep-2) (:intersection keep-1) (:intersection keep-2))
      :union
      (and (and (not (:self keep-1)) (not (:self keep-2))) (:intersection keep-1) (:intersection keep-2))
      :intersection
      (and (and (:self keep-1) (not (:self keep-2))) (and (not (:intersection keep-1)) (not (:intersection keep-2))))
      :left-difference
      (and (and (not (:self keep-1)) (:self keep-2)) (and (not (:intersection keep-1)) (not (:intersection keep-2))))
      :right-difference
      (and (and (:self keep-1) (:self keep-2)) (and (not (:intersection keep-1)) (not (:intersection keep-2))))
      :subtract
      :else :notfound)))


(reg-event-db
  :saved-data/save-operation-results
  (fn [db [_ results]]
    (assoc-in db [:saved-data :editor :results] results)))


(reg-fx
  :perform-op
  (fn [[mine-url q1 q2 op]]
    (go (let [[r1 r2] (<! (operations/operation {:root mine-url} q1 q2))
              results (case op
                        :union (union r1 r2)
                        :intersection (intersection r1 r2)
                        :left-difference (difference r1 r2)
                        :right-difference (difference r2 r1)
                        :subtract (union (difference r1 r2) (difference r2 r1))
                        nil)
              query   {:from   "Gene"
                       :select ["Gene.id"]
                       :where  [{:path   "id"
                                 :op     "ONE OF"
                                 :values (into [] results)}]}]

          (dispatch [:saved-data/save-operation-results query])
          (dispatch [:save-data {:value query
                                 :type  :query
                                 :label "New Datum"}])
          (dispatch [:saved-data/editor-is-editing false])))))

(reg-event-fx
  :saved-data/perform-operation
  (fn [{db :db}]
    (let [[item-1 item-2] (take 2 (get-in db [:saved-data :editor :selected-items]))
          op (determine-op item-1 item-2)
          mine-url (:mine-url db)]
      (let [q1 (assoc
                 (get-in db [:saved-data :items (:id item-1) :value])
                 :select [(:path item-1)]
                 :orderBy nil)
            q2 (assoc
                 (get-in db [:saved-data :items (:id item-2) :value])
                 :select [(:path item-2)]
                 :orderBy nil)]

        ;(println "q1" q1)
        ;(println "q2" q2)

        {:db         (assoc-in db [:saved-data :editor :editing?] true)
         :dispatch   [:saved-data/editor-is-editing true]
         :perform-op [mine-url q1 q2 op]}))))


(reg-event-db
  :save-datum-count
  (fn [db [_ id c]]
    (assoc-in db [:saved-data :items id :count] c)))

(reg-fx
  :count-me
  (fn [[service id query]]
    (go (dispatch [:save-datum-count id (<! (search/raw-query-rows
                                              {:root service}
                                              query
                                              {:format "count"}))]))))

(reg-event-fx
  :saved-data/run-query-count
  (fn [{db :db} [_ [id {query :value}]]]
    (let [mine-url (:mine-url db)]
      {:db       db
       :count-me [mine-url id query]})))

(reg-event-fx
  :saved-data/count-all
  (fn [{db :db}]
    (letfn [(make-event [datum] [:saved-data/run-query-count datum])]
      {:db            db
       :dispatch-many (map make-event (get-in db [:saved-data :items]))})))

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
               (partial map
                        (fn [item]
                          (update-in item [:keep :intersection] not))))))
