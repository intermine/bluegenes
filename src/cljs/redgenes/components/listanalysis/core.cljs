(ns redgenes.components.listanalysis.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]
            [redgenes.components.listanalysis.events]
            [redgenes.components.listanalysis.subs]))

