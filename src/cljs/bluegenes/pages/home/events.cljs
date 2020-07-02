(ns bluegenes.pages.home.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]))

(def root [:home])

(reg-event-db
 :home/select-template-category
 (path root)
 (fn [home [_ category]]
   (assoc home :active-template-category category)))

(reg-event-db
 :home/select-mine-neighbourhood
 (path root)
 (fn [home [_ neighbourhood]]
   (assoc home :active-mine-neighbourhood neighbourhood)))

(reg-event-db
 :home/select-preview-mine
 (path root)
 (fn [home [_ mine-ns]]
   (assoc home :active-preview-mine mine-ns)))
