(ns bluegenes.components.tools.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::all-tools
 (fn [db]
   (get-in db [:tools :all])))

(reg-sub
 ::suitable-tools
 :<- [::all-tools]
 :<- [:panel-params]
 (fn [[tools panel-params]]
   (let [{accept :format, class :type} panel-params]
     (filter #(and (contains? (set (get-in % [:config :accepts])) accept)
                   (contains? (set (get-in % [:config :classes])) class))
             tools))))

