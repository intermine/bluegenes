(ns bluegenes.pages.admin.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :as str]
            [bluegenes.pages.reportpage.subs :as report-subs]))

(reg-sub
 ::root
 (fn [db]
   (:admin db)))

;; Note that this is nil when set to "Default".
(reg-sub
 ::categorize-class
 :<- [::root]
 (fn [admin]
   (:categorize-class admin)))

(reg-sub
 ::categories
 :<- [::root]
 (fn [admin]
   (:categories admin)))

(reg-sub
 ::new-category
 :<- [::root]
 (fn [admin]
   (:new-category admin)))

(reg-sub
 ::available-tool-names
 :<- [:bluegenes.components.tools.subs/installed-tools]
 (fn [tools] ; we do not bother to support filtering by class here
   (->> tools
        (map (fn [tool]
               {:label (get-in tool [:names :human])
                :value (get-in tool [:names :cljs])}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-template-names
 :<- [::report-subs/current-templates]
 :<- [:current-model]
 :<- [::categorize-class]
 (fn [[templates model class]]
   (->> (report-subs/runnable-templates templates model (some-> class name))
        (map (fn [[_ {:keys [title name]}]]
               {:label title :value name}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-class-names
 :<- [:model]
 :<- [::categorize-class]
 (fn [[model class]]
   (->> (if class
          (let [{:keys [references collections]} (get model (keyword class))]
            (->> (concat references collections)
                 (map (fn [[_ {:keys [displayName referencedType]}]]
                        {:label displayName :value referencedType}))))
          (map (fn [[_ {:keys [displayName name]}]]
                 {:label displayName :value name})
               model))
        (sort-by (comp str/lower-case :label)))))
