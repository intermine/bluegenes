(ns re-frame-boiler.handlers
    (:require [re-frame.core :as re-frame :refer [reg-event]]
              [re-frame-boiler.db :as db]))

(reg-event
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))
