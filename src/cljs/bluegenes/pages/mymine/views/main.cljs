(ns bluegenes.pages.mymine.views.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [oops.core :refer [oget ocall]]
            [bluegenes.pages.mymine.views.mymine :as mymine]))

(defn main []
  (fn []
    [mymine/main]))
