(ns redgenes.sections.querybuilder.events
  (:require [re-frame.core :refer [reg-event-db]]
            [clojure.string :refer [join]]))

(reg-event-db
  :qb/set-query
  (fn [db [_ query]]
    (assoc-in db [:qb :query-map] query)))

(reg-event-db
  :qb/add-view
  (fn [db [_ view-vec]]
    (update-in db [:qb :query-map] assoc-in view-vec true)))

(reg-event-db
  :qb/remove-view
  (fn [db [_ view-vec]]
    (-> db
        ; First remove the view
        (update-in [:qb :query-map] update-in (drop-last view-vec) dissoc (last view-vec))
        ; Then remove any constraints
        (update-in [:qb :query-constraints] (partial remove #(= (:path %) (join "." view-vec))))
        )))

(reg-event-db
  :qb/add-constraint
  (fn [db [_ view-vec]]
    (update-in db [:qb :query-constraints] conj {:path  (join "." view-vec)
                                                 :op    "="
                                                 :value nil})))