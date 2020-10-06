(ns bluegenes.components.tools.subs
  (:require [re-frame.core :refer [reg-sub]]
            [bluegenes.utils :refer [suitable-entities]]))

(reg-sub
 ::entity
 (fn [db]
   (get-in db [:tools :entity])))

(reg-sub
 ::entities
 (fn [db]
   (get-in db [:tools :entities])))

(reg-sub
 ::installed-tools
 (fn [db]
   (get-in db [:tools :installed])))

(reg-sub
 ::available-tools
 (fn [db]
   (get-in db [:tools :available])))

(reg-sub
 ::remaining-tools
 :<- [::installed-tools]
 :<- [::available-tools]
 (fn [[installed available]]
   (let [installed-names (set (map #(get-in % [:package :name])
                                   installed))]
     (remove #(contains? installed-names
                         (get-in % [:package :name]))
             available))))

(reg-sub
 ::suitable-tools
 :<- [::installed-tools]
 :<- [::entities]
 :<- [:model]
 :<- [:current-model-hier]
 (fn [[tools entities model hier]]
   (filter #(suitable-entities model hier entities (:config %)) tools)))
