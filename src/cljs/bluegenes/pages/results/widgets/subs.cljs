(ns bluegenes.pages.results.widgets.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :widgets/all-widgets
 (fn [db]
   (get-in db [:results :widget-results])))
