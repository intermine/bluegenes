(ns re-frame-boiler.components.listanalysis.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :listanalysis/results
  (fn [db]
    (-> db :list-analysis :results)))