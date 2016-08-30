(ns redgenes.components.spinner
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]))


(defn main [] [:i.fa.fa-cog.fa-spin.fa-3x.fa-fw])
