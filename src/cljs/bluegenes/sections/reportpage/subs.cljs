(ns bluegenes.sections.reportpage.subs
  (:require [re-frame.core :refer [reg-sub]]
            [imcljs.path :as im-path]
            [clojure.walk :refer [postwalk postwalk-demo]]))

(reg-sub
 ::tools-by-current-type
 (fn [db [_]]
   (let [tool-type (get-in db [:panel-params :type])]
     (get-in db [:tools :classes tool-type]))))

(reg-sub
 ::all-tools
 (fn [db]
   (get-in db [:tools :all])))


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

(comment
  "::runnable-templates:
   Some templates can automatically be run on a report page if they meet certain conditions.
   1. They must have a single editable constraint
   2. That single constraint must be of type LOOKUP
   3. That single constraint must be backed by the same class as the item on the report page")

(reg-sub ::runnable-templates
         :<- [::current-templates]
         :<- [:current-model]
         :<- [:panel-params]
         (fn [[current-templates current-model {report-item-type :type report-item-id :id}]]
           ; Starting with all templates for the current mine...
           (when (and current-templates current-model)
             (->> current-templates
                  ; Only keep ones that meet the following criteria (return something other than nil)
                  (keep (fn [[template-kw {:keys [where tags] :as template-details}]]

                          ; When we only have one editable constraint
                          (when (= 1 (count (filter :editable where)))
                            ; Make a list of indices that are LOOKUP, editable, and
                            ; have a backing class of the report page item type

                            (when-let [replaceable-indexes
                                       (not-empty (keep-indexed (fn [idx {:keys [path op editable :as constraint]}]
                                                                  (when
                                                                    (and
                                                                      (= op "LOOKUP")
                                                                      (= report-item-type (name (im-path/class current-model path)))
                                                                      (= editable true)) idx))
                                                                where))]

                              ; When that list of indices is 1, aka we only have one constraint to change
                              (when (= 1 (count replaceable-indexes))

                                ; Update that particular index with the report page item id and change the op to =
                                ; and also update the path (which ends on a class) to include ".id" on the end.
                                ; Don't make the assumption that it's <report-item-type>.id
                                ; as a query's root might not be the same class as the object on the report page
                                (let [constraint-path (get-in template-details [:where (first replaceable-indexes) :path])]

                                  [template-kw (update-in template-details [:where (first replaceable-indexes)] assoc
                                                          :value report-item-id
                                                          :path (str constraint-path ".id")
                                                          :op "=")]))))))
                  ; And return just the vals
                  (map last)))))


(reg-sub ::current-templates
         :<- [::current-mine]
         :<- [::templates]
         (fn [[current-mine current-templates]]
           (get current-templates current-mine)))


(reg-sub ::non-empty-collections-and-references
          :<- [:current-model]
          :<- [:panel-params]
          :<- [:report]
         (fn [[model params]]
           (let [collections (vals (get-in model [:classes (keyword (:type params)) :collections]))
                references (vals (get-in model [:classes (keyword (:type params)) :references]))
                non-empty-collections (filter (fn [c] (> (get-in model [:classes (keyword (:referencedType c)) :count]) 0)) collections)
                non-empty-references (filter (fn [c] (> (get-in model [:classes (keyword (:referencedType c)) :count]) 0)) references)]
           (concat non-empty-references non-empty-collections))))
