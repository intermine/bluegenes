(ns bluegenes.pages.admin.events
  (:require [re-frame.core :refer [reg-event-db reg-fx reg-event-fx]]
            [re-frame.std-interceptors :refer [path]]
            [bluegenes.utils :refer [addvec remvec dissoc-in]]
            [clojure.string :as str]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]))

(def ^:const category-id-prefix "cat")
(def ^:const child-id-prefix "child")

(def root [:admin])

(reg-event-fx
 ::init
 (fn [{db :db} [_]]
   (let [persisted-cats (get-in db [:mines (:current-mine db) :report-layout])]
     (cond-> {:db (-> db
                      (update-in root dissoc :responses)
                      (assoc-in (concat root [:categories]) persisted-cats)
                      (assoc-in (concat root [:clean-hash]) (hash persisted-cats)))}
       (= :admin.pill/template (get-in db (concat root [:active-pill])))
       (assoc :dispatch-n [[::fetch-template-precomputes]
                           [::fetch-template-summarises]])))))

(reg-event-fx
 ::set-active-pill
 (fn [{db :db} [_ pill]]
   (cond-> {:db (assoc-in db (concat root [:active-pill]) pill)}
     (= :admin.pill/template pill)
     (assoc :dispatch-n [[::fetch-template-precomputes]
                         [::fetch-template-summarises]]))))

(reg-event-db
 ::set-categorize-class
 (path root)
 (fn [admin [_ class-kw]]
   (assoc admin :categorize-class class-kw)))

(defn import-categories
  "Import persisted categories, adding newly generated IDs to categories and children."
  [cats]
  (into {}
        (map (fn [cat-kv]
               (update cat-kv 1
                       (partial mapv (fn [cat]
                                       (-> cat
                                           (assoc :id (gensym category-id-prefix))
                                           (update :children (partial mapv #(assoc % :id (gensym child-id-prefix))))))))))
        cats))

(defn export-categories
  "Export categories for persisting, removing IDs from categories and children."
  [cats]
  (into {}
        (map (fn [cat-kv]
               (update cat-kv 1
                       (partial mapv (fn [cat]
                                       (-> cat
                                           (dissoc :id)
                                           (update :children (partial mapv #(dissoc % :id)))))))))
        cats))

(reg-event-fx
 ::save-layout
 (fn [{db :db} [_ bg-properties-support?]]
   (let [categories (get-in db (concat root [:categories]))]
     (if bg-properties-support?
       {:db (assoc-in db (concat root [:clean-hash]) (hash categories))
        :dispatch [:property/save :layout.report (export-categories categories)
                   {:on-success [::save-layout-success]
                    :on-failure [::save-layout-failure]}]}
       {:db (-> db
                (assoc-in (concat root [:clean-hash]) (hash categories))
                (assoc-in [:mines (:current-mine db) :report-layout] categories))}))))

(reg-event-db
 ::save-layout-success
 (path root)
 (fn [admin [_]]
   (assoc-in admin [:responses :report-layout]
             {:type :success
              :message "Successfully saved changes to report page layout."})))

(reg-event-db
 ::save-layout-failure
 (path root)
 (fn [admin [_ res]]
   (assoc-in admin [:responses :report-layout]
             {:type :failure
              :message (str "Failed to save changes to report page layout. "
                            (not-empty (get-in res [:body :error])))})))

(defn get-categorize-class [admin]
  (get admin :categorize-class))

;; We don't want to repeatedly encode the structure of categories into all our
;; events, so we create utility functions to make things more stratified.

(defn to-categories-path [path admin in-kw & args]
  (let [target-class (get-categorize-class admin)
        in-f (case in-kw
               :get get-in
               :update update-in
               :assoc assoc-in
               :dissoc dissoc-in)]
    (apply in-f admin (concat [:categories target-class] path) args)))

(defn to-categories [admin in-kw & args]
  (apply to-categories-path [] admin in-kw args))

(defn to-category [admin cat-index in-kw & args]
  (apply to-categories-path [cat-index] admin in-kw args))

(defn to-children [admin cat-index in-kw & args]
  (apply to-categories-path [cat-index :children] admin in-kw args))

(defn to-child [admin cat-index child-index in-kw & args]
  (apply to-categories-path [cat-index :children child-index] admin in-kw args))

;; You can think of the above functions as magically being replaced with the
;; correct invocation of get-in, assoc-in or update-in, with the remaining
;; arguments (args) appended to the end.

(defn new-category [cat-name]
  {:category cat-name
   :id (gensym category-id-prefix)
   :children []})

(defn new-child [child & {:keys [collapse]}]
  (assoc child
         :id (gensym child-id-prefix)
         :collapse collapse))

(reg-event-db
 ::category-add
 (path root)
 (fn [admin [_ category-name]]
   (-> admin
       (to-categories :update (fnil conj []) (new-category category-name))
       (update :new-category empty))))

(reg-event-db
 ::category-remove
 (path root)
 (fn [admin [_ cat-index]]
   (let [cats (to-categories admin :get)
         cats (remvec cats cat-index)]
     (if (empty? cats)
       (to-categories admin :dissoc)
       (to-categories admin :assoc cats)))))

(reg-event-db
 ::category-move-up
 (path root)
 (fn [admin [_ cat-index]]
   (if (zero? cat-index)
     admin
     (let [categories (to-categories admin :get)
           cat (nth categories cat-index)]
       (to-categories admin :assoc
                      (-> categories
                          (remvec cat-index)
                          (addvec (dec cat-index) cat)))))))

(reg-event-db
 ::category-move-down
 (path root)
 (fn [admin [_ cat-index]]
   (let [categories (to-categories admin :get)
         last-index (dec (count categories))]
     (if (>= cat-index last-index)
       admin
       (let [cat (nth categories cat-index)]
         (to-categories admin :assoc
                        (-> categories
                            (remvec cat-index)
                            (addvec (inc cat-index) cat))))))))

(reg-event-db
 ::category-rename
 (path root)
 (fn [admin [_ cat-index new-name]]
   (to-category admin cat-index :update assoc :category new-name)))

(reg-event-db
 ::set-new-category
 (path root)
 (fn [admin [_ new-category]]
   (assoc admin :new-category new-category)))

(reg-event-db
 ::children-add
 (path root)
 (fn [admin [_ cat-index children]]
   (to-children admin cat-index :update
                (fnil into [])
                (map new-child children))))

(reg-event-db
 ::child-remove
 (path root)
 (fn [admin [_ cat-index child-index]]
   (to-children admin cat-index :update
                remvec child-index)))

(reg-event-db
 ::child-move-up
 (path root)
 (fn [admin [_ cat-index child-index]]
   (if (zero? child-index)
     admin
     (let [children (to-children admin cat-index :get)
           child (nth children child-index)]
       (to-children admin cat-index :assoc
                    (-> children
                        (remvec child-index)
                        (addvec (dec child-index) child)))))))

(reg-event-db
 ::child-move-down
 (path root)
 (fn [admin [_ cat-index child-index]]
   (let [children (to-children admin cat-index :get)
         last-index (dec (count children))]
     (if (>= cat-index last-index)
       admin
       (let [child (nth children child-index)]
         (to-children admin cat-index :assoc
                      (-> children
                          (remvec child-index)
                          (addvec (inc child-index) child))))))))

(reg-event-db
 ::child-set-collapse
 (path root)
 (fn [admin [_ cat-index child-index state]]
   (to-child admin cat-index child-index :update
             assoc :collapse state)))

(reg-event-db
 ::child-set-description
 (path root)
 (fn [admin [_ cat-index child-index text]]
   (if (seq text)
     (to-child admin cat-index child-index :update
               assoc :description text)
     (to-child admin cat-index child-index :update
               dissoc :description))))

(reg-event-db
 ::clear-notice-response
 (path root)
 (fn [admin [_]]
   (update admin :responses dissoc :notice)))

(reg-event-fx
 ::save-notice
 (fn [{db :db} [_ new-notice]]
   (if (str/blank? new-notice)
     {:dispatch [:property/delete :notice
                 {:on-success [::save-notice-success]
                  :on-failure [::save-notice-failure]}]}
     {:dispatch [:property/save :notice new-notice
                 {:on-success [::save-notice-success]
                  :on-failure [::save-notice-failure]}]})))

(reg-event-db
 ::save-notice-success
 (path root)
 (fn [admin [_]]
   (assoc-in admin [:responses :notice]
             {:type :success
              :message "Successfully saved changes to notice text."})))

(reg-event-db
 ::save-notice-failure
 (path root)
 (fn [admin [_ res]]
   (assoc-in admin [:responses :notice]
             {:type :failure
              :message (str "Failed to save changes to notice text. "
                            (not-empty (get-in res [:body :error])))})))

;; Manage templates

(def manage-templates (into root [:manage-templates]))

(reg-event-db
 ::set-template-filter
 (path manage-templates)
 (fn [tmpl [_ text]]
   (assoc tmpl :template-filter text)))

(reg-event-db
 ::check-template
 (path manage-templates)
 (fn [tmpl [_ template-name]]
   (update tmpl :checked-templates (fnil conj #{}) template-name)))

(reg-event-db
 ::uncheck-template
 (path manage-templates)
 (fn [tmpl [_ template-name]]
   (update tmpl :checked-templates (fnil disj #{}) template-name)))

(reg-event-db
 ::check-all-templates
 (path manage-templates)
 (fn [tmpl [_ authorized-templates]]
   (assoc tmpl :checked-templates (into #{} (map :name (vals authorized-templates))))))

(reg-event-db
 ::uncheck-all-templates
 (path manage-templates)
 (fn [tmpl [_ _authorized-templates]]
   (assoc tmpl :checked-templates #{})))

(reg-event-fx
 ::fetch-template-precomputes
 (fn [{db :db} [_]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (fetch/precompute service)
                :on-success [::fetch-template-properties-success :precomputes]
                :on-failure [::fetch-template-properties-failure :precomputes]}})))

(reg-event-fx
 ::fetch-template-summarises
 (fn [{db :db} [_]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:im-chan {:chan (fetch/summarise service)
                :on-success [::fetch-template-properties-success :summarises]
                :on-failure [::fetch-template-properties-failure :summarises]}})))

(reg-event-db
 ::fetch-template-properties-success
 (path manage-templates)
 (fn [tmpl [_ kw templates]]
   (assoc tmpl kw templates)))

(reg-event-fx
 ::fetch-template-properties-failure
 (fn [{db :db} [_ kw res]]
   {:dispatch [:messages/add
               {:markup [:span (str "Failed to acquire template " (name kw) ". ")
                         (when-let [err (get-in res [:body :error])]
                           [:code err])
                         " You can still precompute/summarise templates, but if it's been done for a template in the past, it won't show up as so."]
                :style "warning"}]}))

(reg-event-fx
 ::precompute-template
 (fn [{db :db} [_ template-name]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (assoc-in db (concat manage-templates [:precomputes (keyword template-name)])
                    :in-progress)
      :im-chan {:chan (save/precompute service template-name)
                :on-success [::template-action-success :precomputes]
                :on-failure [::template-action-failure :precomputes template-name]}})))

(reg-event-fx
 ::summarise-template
 (fn [{db :db} [_ template-name]]
   (let [service (get-in db [:mines (:current-mine db) :service])]
     {:db (assoc-in db (concat manage-templates [:summarises (keyword template-name)])
                    :in-progress)
      :im-chan {:chan (save/summarise service template-name)
                :on-success [::template-action-success :summarises]
                :on-failure [::template-action-failure :summarises template-name]}})))

(reg-event-db
 ::template-action-success
 (path manage-templates)
 (fn [tmpl [_ kw templatem]]
   (update tmpl kw merge templatem)))

(reg-event-fx
 ::template-action-failure
 (fn [{db :db} [_ kw template-name res]]
   {:dispatch [:messages/add
               {:markup [:span
                         (case kw
                           :precomputes "Failed to precompute "
                           :summarises "Failed to summarise ")
                         [:em template-name] ". "
                         [:code (or (not-empty (get-in res [:body :error]))
                                    "Please try again later.")]]}]}))
