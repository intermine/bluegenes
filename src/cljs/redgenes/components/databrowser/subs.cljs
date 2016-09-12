(ns redgenes.components.databrowser.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :databrowser/whitelist
  (fn [db _]
    (:databrowser/whitelist db)))
