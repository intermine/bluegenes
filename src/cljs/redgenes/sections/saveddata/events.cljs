(ns redgenes.sections.saveddata.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.filters :as im-filters]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as t]))

(defn get-parts
  [model query]
  (group-by :type
            (distinct
              (map (fn [path]
                     (assoc {}
                       :type (im-filters/im-type model path)
                       :path (str (im-filters/trim-path-to-class model path) ".id")))
                   (:select query)))))


(reg-event-db
  :saved-data/calculate-parts
  (fn [db]
    (let [model (get-in db [:assets :model])
          items (get-in db [:saved-data :items])]
      (println "DO" (im-filters/im-type model "Gene.homologues")))
    db))

(reg-event-fx
  :saved-data/toggle-edit-mode
  (fn [{db :db}]
    {:db       (update-in db [:saved-data :list-operations-enabled] not)
     :dispatch [:saved-data/calculate-parts]}))

(reg-event-fx
  :save-data
  (fn [{db :db} [_ data]]
    (let [new-id (str (uuid/make-random-uuid))
          model  (get-in db [:assets :model])]
      {:db       (assoc-in db [:saved-data :items new-id]
                           (-> data
                               (merge {:created (t/now)
                                       :updated (t/now)
                                       :parts   (get-parts model (:value data))})))
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