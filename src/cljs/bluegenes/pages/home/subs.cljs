(ns bluegenes.pages.home.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :as str]))

(reg-sub
 :home/root
 (fn [db]
   (:home db)))

(reg-sub
 :home/active-template-category
 :<- [:home/root]
 (fn [home]
   (:active-template-category home)))

(reg-sub
 :home/active-mine-neighbourhood
 :<- [:home/root]
 (fn [home]
   (:active-mine-neighbourhood home)))

(reg-sub
 :home/all-registry-mine-neighbourhoods
 :<- [:registry]
 (fn [registry]
   (->> registry
        (mapcat (comp :neighbours val))
        (set)
        (sort)
        (cons "All"))))

(reg-sub
 :home/mines-by-neighbourhood
 :<- [:registry-with-default]
 :<- [:home/active-mine-neighbourhood]
 (fn [[registry active-neighbourhood]]
   (cond->> (sort-by #(or (-> % val :name)
                          (-> % key name str/capitalize))
                     registry)
     ((every-pred not-empty (partial not= "All")) active-neighbourhood)
     (filter #(contains? (-> % val :neighbours set) active-neighbourhood)))))

(reg-sub
 :home/active-preview-mine
 :<- [:home/root]
 (fn [home]
   (:active-preview-mine home)))

;; Note that a random preview mine from the active neighbourhood filter
;; will be returned if one hasn't been selected by the user. Think of this
;; like showcasing what's available.
(reg-sub
 :home/preview-mine
 :<- [:home/active-preview-mine]
 :<- [:registry-with-default]
 :<- [:home/mines-by-neighbourhood]
 (fn [[active-preview-mine registry sorted-mines]]
   (get registry active-preview-mine (-> sorted-mines rand-nth val))))
