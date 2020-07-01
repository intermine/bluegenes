(ns bluegenes.pages.home.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
 :home/select-template-category
 (fn [db [_ category]]
   (assoc-in db [:home :active-template-category] category)))
