(ns bluegenes.components.tools.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::entity
 (fn [db]
   (get-in db [:tools :entity])))

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
 :<- [::entity]
 :<- [:model]
 (fn [[tools entity model]]
   (when-let [{:keys [format class]} entity]
     (filter (fn [{{:keys [accepts classes depends]} :config :as _tool}]
               (and (contains? (set accepts) format)
                    (contains? (set classes) class)
                    (every? #(contains? model %) (map keyword depends))))
             tools))))
