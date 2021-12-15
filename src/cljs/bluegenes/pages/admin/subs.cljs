(ns bluegenes.pages.admin.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [clojure.string :as str]
            [bluegenes.utils :refer [suitable-entities template-contains-string?]]
            [bluegenes.pages.reportpage.utils :as report-utils]
            [bluegenes.pages.querybuilder.views :refer [sort-classes filter-preferred]]))

(reg-sub
 ::root
 (fn [db]
   (:admin db)))

(reg-sub
 ::active-pill
 :<- [::root]
 (fn [admin]
   (:active-pill admin)))

(reg-sub
 ::responses
 :<- [::root]
 (fn [admin [_ kw]]
   (get-in admin [:responses kw])))

(reg-sub
 ::categorize-class
 :<- [::root]
 (fn [admin]
   (:categorize-class admin)))

(reg-sub
 ::categorize-options
 :<- [:model]
 (fn [model]
   (let [classes (sort-classes model)
         preferred (filter-preferred classes)]
     (if (seq preferred)
       (concat preferred [[:separator]] classes)
       classes))))

(reg-sub
 ::categories
 :<- [::root]
 :<- [::categorize-class]
 (fn [[admin categorize]]
   (get-in admin [:categories categorize])))

(reg-sub
 ::new-category
 :<- [::root]
 (fn [admin]
   (:new-category admin)))

(reg-sub
 ::dirty?
 :<- [::root]
 (fn [admin]
   (not= (:clean-hash admin) (hash (:categories admin)))))

(reg-sub
 ::available-tool-names
 :<- [:bluegenes.components.tools.subs/installed-tools]
 :<- [:model]
 :<- [:current-model-hier]
 :<- [:current-mine-is-env?]
 (fn [[tools model-classes model-hier env-mine?] [_ class]]
   ;; Only show visualizations for configured mines (i.e. not registry mines).
   (when env-mine?
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
          (sort-by (comp str/lower-case :label))))))

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
                 (map (fn [[_ {:keys [displayName name]}]]
                        {:label displayName
                         :value name
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

;; Manage templates

(reg-sub
 ::manage-templates
 :<- [::root]
 (fn [admin]
   (:manage-templates admin)))

(reg-sub
 ::template-filter
 :<- [::manage-templates]
 (fn [manage-templates]
   (:template-filter manage-templates)))

(reg-sub
 ::authorized-templates
 :<- [:templates]
 (fn [templates]
   (->> templates
        (filter (comp :authorized val)))))

(reg-sub
 ::filtered-templates
 :<- [::authorized-templates]
 :<- [::template-filter]
 (fn [[authorized-templates text-filter]]
   (->> authorized-templates
        (filter (partial template-contains-string? text-filter))
        (vals)
        (sort-by (comp str/lower-case :name)))))

(reg-sub
 ::checked-templates
 :<- [::manage-templates]
 (fn [manage-templates]
   (:checked-templates manage-templates)))

(reg-sub
 ::template-checked?
 :<- [::checked-templates]
 (fn [checked-templates [_ template-name]]
   (contains? checked-templates template-name)))

(reg-sub
 ::all-templates-checked?
 :<- [::checked-templates]
 :<- [::authorized-templates]
 (fn [[checked-templates authorized-templates] [_]]
   (= (count checked-templates) (count authorized-templates))))

(reg-sub
 ::precomputes
 :<- [::manage-templates]
 (fn [manage-templates]
   (:precomputes manage-templates)))

(reg-sub
 ::summarises
 :<- [::manage-templates]
 (fn [manage-templates]
   (:summarises manage-templates)))

(reg-sub
 ::all-template-tags
 :<- [:templates]
 (fn [all-templates]
   (->> all-templates
        (mapcat (fn [[_ {:keys [tags]}]] tags))
        (set))))

;; Modal

(reg-sub
 ::modal
 :<- [::root]
 (fn [admin]
   (:modal admin)))
