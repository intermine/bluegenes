(ns bluegenes.components.viz.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :viz/results
 (fn [db [_]]
   (get-in db [:results :viz])))
