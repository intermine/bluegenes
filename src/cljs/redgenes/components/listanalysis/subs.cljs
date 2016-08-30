(ns redgenes.components.listanalysis.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(reg-sub
  :listanalysis/results
  (fn [db [_ widget]]
    (-> db :list-analysis :results widget)))

(reg-sub
  :listanalysis/results-all
  (fn [db _]
    (-> db :list-analysis :results)))