(ns re-frame-boiler.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

(reg-sub
  :active-panel
  (fn [db _]
    (:active-panel db)))

(reg-sub
  :app-db
  (fn [db _] db))