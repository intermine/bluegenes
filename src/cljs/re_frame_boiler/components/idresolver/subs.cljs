(ns re-frame-boiler.components.idresolver.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :bank
  (fn [db]
    (-> db :idresolver :bank)))