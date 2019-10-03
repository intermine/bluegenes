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
 :<- [:model]
 (fn [[tools entity model]]
   (let [{:keys [format class]} entity]
     (filter (fn [{{:keys [accepts classes depends]} :config :as _tool}]
               (and (contains? (set accepts) format)
                    (contains? (set classes) class)
                    (every? #(contains? model %) (map keyword depends))))
             tools))))
