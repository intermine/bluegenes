(ns bluegenes.components.tools.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::entity
 (fn [db]
   (get-in db [:tools :entity])))

(reg-sub
 ::all-tools
 (fn [db]
   (get-in db [:tools :all])))

(reg-sub
 ::suitable-tools
 :<- [::all-tools]
 :<- [::entity]
 (fn [[tools entity]]
   (let [{:keys [format class]} entity]
     (filter #(and (contains? (set (get-in % [:config :accepts])) format)
                   (contains? (set (get-in % [:config :classes])) class))
             tools))))
