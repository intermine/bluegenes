(ns bluegenes.sections.reportpage.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.walk :refer [postwalk postwalk-demo]]))

(reg-sub
 ::tools-by-current-type
 (fn [db [_]]
   (let [tool-type (get-in db [:panel-params :type])]
     (get-in db [:tools :classes tool-type]))))

(reg-sub
 ::all-tools
 (fn [db]
   (get-in db [:tools :all])))
