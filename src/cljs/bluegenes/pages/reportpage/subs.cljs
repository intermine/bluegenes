(ns bluegenes.pages.reportpage.subs
  (:require [re-frame.core :refer [reg-sub]]
            [imcljs.path :as im-path]
            [clojure.string :as string]
            [bluegenes.pages.reportpage.utils :as utils]
            [bluegenes.components.tools.subs :as tools-subs]))

(reg-sub
 ::report
 (fn [db]
   (:report db)))

(reg-sub
 ::report-summary
 :<- [::report]
 (fn [report]
   (:summary report)))

(reg-sub
 ::report-title
 :<- [::report]
 (fn [report]
   (:title report)))

(reg-sub ::a-table
         (fn [db [_ location]]
           (get-in db location)))

(reg-sub ::current-mine
         (fn [db]
           (get db :current-mine)))

(reg-sub ::templates
         (fn [db]
           (get-in db [:assets :templates])))

(reg-sub ::current-templates
         :<- [::current-mine]
         :<- [::templates]
         (fn [[current-mine current-templates]]
           (get current-templates current-mine)))

(reg-sub
 ::fasta
 :<- [::report]
 (fn [report]
   (:fasta report)))

(reg-sub
 ::fasta-identifier
 :<- [::fasta]
 (fn [fasta]
   (-> (string/split fasta #"[ \n]")
       (first)
       (subs 1))))

(reg-sub
 ::chromosome-location
 :<- [::fasta]
 (fn [fasta]
   (second (string/split fasta #"[ \n]"))))

(reg-sub
 ::fasta-length
 :<- [::fasta]
 (fn [fasta]
   (->> (string/split-lines fasta)
        rest
        (apply str)
        count)))

(reg-sub
 ::refs+colls
 :<- [:current-model]
 :<- [:panel-params]
 (fn [[model params]]
   (let [{:keys [classes]} model
         object-kw (-> params :type keyword)]
     (concat (vals (get-in classes [object-kw :collections]))
             (vals (get-in classes [object-kw :references]))))))

(reg-sub
 ::refs+colls-by-referencedType
 :<- [::refs+colls]
 (fn [refs+colls]
   (group-by :referencedType refs+colls)))

(reg-sub
 ::a-ref+coll
 :<- [::refs+colls-by-referencedType]
 (fn [refs+colls [_ referencedType]]
   ;; If there are multiple identical referencedType among one class' combined
   ;; references and collections, this could lead to only one of them being
   ;; displayed (we should verify if this is possible).
   (first (get refs+colls (name referencedType)))))

(reg-sub
 ::a-template
 :<- [:current-model]
 :<- [:panel-params]
 :<- [::current-templates]
 (fn [[model params templates] [_ template-name]]
   (let [{object-type :type object-id :id} params]
       (->> (get templates (keyword template-name))
            (utils/init-template model object-type object-id)))))

(reg-sub
 ::a-tool
 :<- [::tools-subs/installed-tools-by-id]
 (fn [tools-by-id [_ tool-cljs-name]]
   (get tools-by-id tool-cljs-name)))
