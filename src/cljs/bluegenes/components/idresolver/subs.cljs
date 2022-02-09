(ns bluegenes.components.idresolver.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :refer [blank?]]))

(defn not-blank [s]
  (if-not (blank? s) s nil))

(reg-sub ::staged-files
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :files]))))

(reg-sub ::textbox-identifiers
         (fn [db]
           (not-blank (get-in db [:idresolver :stage :textbox]))))

(reg-sub ::stage-options
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :options]))))

(reg-sub ::stage-status
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :status]))))

(reg-sub ::stage-flags
         (fn [db]
           (not-empty (get-in db [:idresolver :stage :flags]))))

(reg-sub ::resolution-response
         (fn [db]
           (not-empty (get-in db [:idresolver :response]))))

(reg-sub ::resolution-error
         (fn [db]
           (get-in db [:idresolver :error])))

(reg-sub ::list-name
         (fn [db]
           (get-in db [:idresolver :save :list-name])))

(reg-sub ::resolution-stats
         (fn [db]
           (get-in db [:idresolver :response :stats])))

(reg-sub ::review-tab
         (fn [db]
           (get-in db [:idresolver :stage :options :review-tab])))

(reg-sub ::upload-tab
         (fn [db]
           (get-in db [:idresolver :stage :options :upload-tab])))

(reg-sub ::view
         (fn [db]
           (get-in db [:idresolver :stage :view])))

;; Note: When making changes to this, make the same change in :bluegenes.components.idresolver.events/redirect-missing-resolution
(reg-sub ::parsing?
         (fn [db]
           (= (get-in db [:idresolver :stage :status :action]) :parsing)))

;; Note: When making changes to this, make the same change in :bluegenes.components.idresolver.events/redirect-missing-resolution
(reg-sub ::parsed?
         (fn [db]
           (boolean (get-in db [:idresolver :stage :flags :parsed]))))

(reg-sub
 ::in-progress?
 :<- [::parsing?]
 :<- [::parsed?]
 (fn [[parsing? parsed?]]
   (or parsing? parsed?)))

(reg-sub
 ::stats
 :<- [::resolution-response]
 (fn [resolution-response]
   (let [{{{:keys [matches issues notFound all]} :identifiers :as s} :stats}
         resolution-response
         {{:keys [OTHER WILDCARD DUPLICATE TYPE_CONVERTED MATCH]} :matches}
         resolution-response]
     {:matches matches
      :issues issues
      :notFound notFound
      :all all
      :duplicates (count DUPLICATE)
      :converted (count TYPE_CONVERTED)
      :other (count OTHER)})))

;;if the example doesn't exist, we don't want to show the "example"
;; button to users. 
(reg-sub
 ::example?
 (fn [db]
   (let [current-mine (get db :current-mine)]
     (some? (get-in db [:mines current-mine :idresolver-example])))))
