(ns bluegenes.pages.reportpage.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [imcljs.path :as im-path]
            [clojure.string :as string]
            [bluegenes.pages.reportpage.utils :as utils]
            [bluegenes.components.tools.subs :as tools-subs]
            [bluegenes.pages.templates.helpers :refer [prepare-template-query]]))

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
 ::report-error
 :<- [::report]
 (fn [report]
   (:error report)))

(reg-sub
 ::report-title
 :<- [::report]
 (fn [report]
   (:title report)))

(reg-sub
 ::report-lists
 :<- [::report]
 (fn [report]
   (:lists report)))

(reg-sub
 ::report-external-links
 :<- [::report]
 (fn [report]
   (->> (:external-links report)
        (vals)
        (filter :url)
        (sort-by (comp string/lower-case :title)))))

(reg-sub
 ::report-homologues
 :<- [::report]
 (fn [report]
   (:homologues report)))

(reg-sub
 ::report-sources
 :<- [::report]
 (fn [report]
   (:sources report)))

(reg-sub
 ::report-active-toc
 :<- [::report]
 (fn [report]
   (:active-toc report)))

(reg-sub
 ::report-filter-text
 :<- [::report]
 (fn [report]
   (or (:filter-text report) "")))

(reg-sub
 ::a-table
 (fn [db [_ location]]
   (get-in db location)))

;; This can be a string with the fasta response or :too-long
(reg-sub
 ::fasta
 :<- [::report]
 (fn [report]
   (:fasta report)))

(reg-sub
 ::fasta-identifier
 :<- [::fasta]
 (fn [fasta]
   (when (string? fasta)
     (-> (string/split fasta #"[ \n]")
         (first)
         (subs 1)))))

(reg-sub
 ::chromosome-location
 :<- [::fasta]
 (fn [fasta]
   (when (string? fasta)
      ;; This handles the case where Chromosome Location is missing entirely (like on Proteins).
     (let [loc (-> (string/split-lines fasta)
                   (first)
                   (string/split #"[ \n]")
                   (second))]
       (if (= loc "-")
         ;; Chromosome FASTA have a dash where you'd expect the chromosome location.
         ;; For now, we only want to avoid breakage, so we pretend it doesn't have one.
         nil
         loc)))))

;; TODO do not calculate manually and instead use :length ?
(reg-sub
 ::fasta-length
 :<- [::fasta]
 (fn [fasta]
   (when (string? fasta)
     (->> (string/split-lines fasta)
          rest
          (apply str)
          count))))

(reg-sub
 ::refs+colls
 :<- [:current-model]
 :<- [:panel-params]
 (fn [[model params]]
   (let [{:keys [classes]} model
         object-kw (-> params :type keyword)]
     ;; This merge assumes there are no identical keys across collections and references.
     (merge (get-in classes [object-kw :collections])
            (get-in classes [object-kw :references])))))

(reg-sub
 ::a-ref+coll
 :<- [::refs+colls]
 (fn [refs+colls [_ name]]
   (get refs+colls (keyword name))))

(reg-sub
 ::a-template
 :<- [:current-model]
 :<- [:panel-params]
 :<- [:templates]
 (fn [[model params templates] [_ template-name]]
   (let [{object-type :type object-id :id} params]
     (->> (get templates (keyword template-name))
          (utils/init-template model object-type object-id)
          (prepare-template-query)))))

(reg-sub
 ::a-tool
 :<- [::tools-subs/installed-tools-by-id]
 (fn [tools-by-id [_ tool-cljs-name]]
   (get tools-by-id tool-cljs-name)))

(reg-sub
 ::tool-for-class?
 (fn [[_ class]]
   (subscribe [:bluegenes.pages.admin.subs/available-tool-names class]))
 (fn [tools [_ _class]]
   (into #{} (map :value tools))))

(reg-sub
 ::template-for-class?
 (fn [[_ class]]
   (subscribe [:bluegenes.pages.admin.subs/available-template-names class]))
 (fn [templates [_ _class]]
   (into #{} (map :value templates))))

(reg-sub
 ::ref+coll-for-class?
 (fn [[_ class]]
   (subscribe [:bluegenes.pages.admin.subs/available-class-names class]))
 (fn [refs+colls [_ _class]]
   (into #{} (map :value refs+colls))))

(reg-sub
 ::share
 :<- [::report]
 (fn [report]
   (:share report)))

(reg-sub
 ::strand
 :<- [::report]
 (fn [report]
   (:strand report)))
