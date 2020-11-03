(ns bluegenes.pages.admin.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [bluegenes.utils :refer [suitable-entities]]
            [bluegenes.pages.reportpage.utils :as report-utils]))

(reg-sub
 ::root
 (fn [db]
   (:admin db)))

(reg-sub
 ::responses
 :<- [::root]
 (fn [admin [_ kw]]
   (get-in admin [:responses kw])))

;; Note that this is nil when set to "Default".
(reg-sub
 ::categorize-class
 :<- [::root]
 (fn [admin]
   (:categorize-class admin)))

(reg-sub
 ::categories
 :<- [::root]
 :<- [::categorize-class]
 (fn [[admin categorize]]
   (get-in admin [:categories (or categorize :default)])))

(reg-sub
 ::new-category
 :<- [::root]
 (fn [admin]
   (:new-category admin)))

(reg-sub
 ::available-tool-names
 :<- [:bluegenes.components.tools.subs/installed-tools]
 :<- [:model]
 :<- [:current-model-hier]
 (fn [[tools model-classes model-hier] [_ class]]
   (->> (cond->> tools
          (not-empty class)
          (filter #(suitable-entities model-classes
                                      model-hier
                                      {(keyword class) {:class (name class)
                                                        :format "id"}}
                                      (:config %))))
        (map (fn [tool]
               {:label (get-in tool [:names :human])
                :value (get-in tool [:names :cljs])
                :type "tool"}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-template-names
 :<- [:templates]
 :<- [:current-model]
 (fn [[templates model] [_ class]]
   (->> (report-utils/runnable-templates templates model (some-> class name))
        (map (fn [[_ {:keys [title name]}]]
               {:label title
                :value name
                :type "template"}))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::available-class-names
 :<- [:model]
 (fn [model [_ class]]
   (->> (if class
          (let [{:keys [references collections]} (get model (keyword class))]
            (->> (concat references collections)
                 (map (fn [[_ {:keys [displayName referencedType]}]]
                        {:label displayName
                         :value referencedType
                         :type "class"}))))
          (map (fn [[_ {:keys [displayName name]}]]
                 {:label displayName
                  :value name
                  :type "class"})
               model))
        (sort-by (comp str/lower-case :label)))))

(reg-sub
 ::categories-fallback
 (fn [[_ class]]
   [(subscribe [::available-tool-names class])
    (subscribe [::available-template-names class])
    (subscribe [::available-class-names class])])
 (fn [[tools templates classes] [_ _class]]
   (report-utils/fallback-layout tools classes templates)))
