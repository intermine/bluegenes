(ns bluegenes.pages.querybuilder.subs
  (:require [re-frame.core :refer [reg-sub]]
            [clojure.string :refer [join blank?]]
            [bluegenes.pages.querybuilder.logic :as con-logic]))

(reg-sub
 :qb/constraint-logic
 (fn [db]
   (join "" (drop-last (rest (apply vector (str (con-logic/vec->list (get-in db [:qb :constraint-logic])))))))))

(reg-sub
 :qb/root-class
 (fn [db]
   (get-in db [:qb :root-class])))

(reg-sub
 :qb/enhance-query
 (fn [db]
   (get-in db [:qb :enhance-query])))

(reg-sub
 :qb/constraint-count
 :<- [:qb/enhance-query]
 (fn [enhance-query]
   (->> enhance-query
        (tree-seq map? vals)
        (mapcat :constraints)
        (keep :code)
        (count))))

(reg-sub
 :qb/preview
 (fn [db]
   (get-in db [:qb :preview])))

(reg-sub
 :qb/preview-error
 (fn [db]
   (get-in db [:qb :preview-error])))

(reg-sub
 :qb/fetching-preview?
 (fn [db]
   (get-in db [:qb :fetching-preview?])))

(reg-sub
 :qb/im-query
 (fn [db]
   (get-in db [:qb :im-query])))

(reg-sub
 :qb/order
 (fn [db]
   (get-in db [:qb :order])))

(reg-sub
 :qb/menu
 (fn [db]
   (get-in db [:qb :menu])))

(reg-sub
 :qb/example
 (fn [db]
   (get-in db [:mines (get db :current-mine) :default-query-example])))

(reg-sub
 :qb/saved-queries
 (fn [db]
   (sort-by key (get-in db [:qb :saved-queries]))))

(reg-sub
 :qb/import-result
 (fn [db]
   (get-in db [:qb :import-result])))

(reg-sub
 :qb/active-sort
 (fn [db [_ path]]
   (->> (get-in db [:qb :sort])
        (filter (comp #{path} :path))
        (first)
        (:direction))))

(reg-sub
 :qb/sort-priority
 ;; Returns either nil or the index (sort priority) of `path`.
 (fn [db [_ path]]
   (first (keep-indexed (fn [index item]
                          (when (= path (:path item))
                            index))
                        (get-in db [:qb :sort])))))

(reg-sub
 :qb/active-outer-join
 (fn [db [_ path]]
   (contains? (get-in db [:qb :joins]) path)))

(reg-sub
 :qb/joins
 (fn [db]
   (get-in db [:qb :joins])))

(reg-sub
 :qb/im-query-constraints
 :<- [:qb/im-query]
 (fn [im-query]
   (:where im-query)))

(reg-sub
 :qb/menu-type-constraints
 :<- [:qb/menu]
 (fn [qb-menu]
   (con-logic/qb-menu->type-constraints qb-menu)))

(reg-sub
 :qb/template-meta
 (fn [db]
   (get-in db [:qb :template-meta])))

(reg-sub
 :qb/template-in-progress?
 :<- [:qb/template-meta]
 (fn [template-meta]
   (if (map? template-meta)
     (boolean (some (comp not-empty template-meta)
                    [:name :title :description :comment]))
     false)))
