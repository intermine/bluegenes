(ns bluegenes.pages.home.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [bluegenes.events.blog :refer [get-rss-from-db]]))

(reg-sub
 :home/root
 (fn [db]
   (:home db)))

(reg-sub
 :home/feedback-response
 :<- [:home/root]
 (fn [home]
   (:feedback-response home)))

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
 :<- [:registry+configured-mines]
 :<- [:home/active-mine-neighbourhood]
 (fn [[mines active-neighbourhood]]
   (cond->> (sort-by #(or (-> % val :name)
                          (-> % key name str/capitalize))
                     mines)
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
 :<- [:registry+configured-mines]
 :<- [:home/mines-by-neighbourhood]
 (fn [[active-preview-mine mines sorted-mines]]
   (get mines active-preview-mine (-> sorted-mines rand-nth val))))

;; Be wary that this can return `false`, which many seq functions throw on.
(reg-sub
 :home/latest-posts
 (fn [db]
   (let [rss (get-rss-from-db db)]
     (get-in db [:cache :rss rss]))))

(reg-sub
 :home/customisations
 :<- [:current-mine/customisation]
 (fn [custom]
   (:homepage custom)))

(reg-sub
 :home/customisation
 :<- [:home/customisations]
 (fn [home [_ kw]]
   (get home kw)))

(reg-sub
 :home/custom-cta
 (fn [_]
   (subscribe [:home/customisation :cta]))
 (fn [cta]
   cta))
