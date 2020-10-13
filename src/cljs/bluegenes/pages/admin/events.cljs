(ns bluegenes.pages.admin.events
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]))

(def root [:admin])

(reg-event-db
 ::set-categorize-class
 (path root)
 (fn [admin [_ class-kw]]
   (assoc admin :categorize-class class-kw)))
