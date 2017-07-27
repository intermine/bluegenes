(ns bluegenes.sections.mymine.views.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]))

(defn main []
  (fn []
    [:div.container [:h1 "MyMine"]]))
