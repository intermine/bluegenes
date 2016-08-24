(ns re-frame-boiler.components.templates.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch]]))

(reg-event-db
  :select-template
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :selected-template] id)))

(reg-event-db
  :select-template-category
  (fn [db [_ id]]
    (assoc-in db [:components :template-chooser :selected-template-category] id)))