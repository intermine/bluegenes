(ns bluegenes.events.bgproperties
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [cljs.reader :as reader]
            [bluegenes.pages.admin.events :refer [import-categories]]))

(defn read-prop [value]
  (reader/read-string value))

(defn write-prop [value]
  (pr-str value))

(defn bg-properties-to-bluegenes
  "Returns a map generated from `bg-properties` to be merged with the current
  mine. Note that if a property is missing, its value will be nil. This is to
  ensure that deleted keys get their previous value overwritten with nil."
  [bg-properties]
  {:report-layout (some-> (get bg-properties :layout.report) read-prop import-categories)})

;; TODO Should we check that you're admin first at the clientside?

;; TODO allow passing on-success and on-failure event vectors to :property/save and :property/delete to be dispatched on success

(reg-event-fx
 :property/save
 (fn [{db :db} [_ key value]]
   (let [mine-kw (get db :current-mine)
         old-props (get-in db [:assets :bg-properties mine-kw])
         service (get-in db [:mines mine-kw :service])
         value-edn (write-prop value)]
     (cond
       (and (contains? old-props key)
            (= (get old-props key) value-edn))
       {} ; No changes made; do nothing!

       (contains? old-props key)
       {:im-chan {:chan (save/update-bluegenes-properties service (name key) value-edn)
                  :on-success [:property/success key value-edn]
                  :on-failure [:property/failure key]}}

       :else
       {:im-chan {:chan (save/bluegenes-properties service (name key) value-edn)
                  :on-success [:property/success key value-edn]
                  :on-failure [:property/failure key]}}))))

(reg-event-fx
 :property/delete
 (fn [{db :db} [_ key]]
   (let [mine-kw (get db :current-mine)
         old-props (get-in db [:assets :bg-properties mine-kw])
         service (get-in db [:mines mine-kw :service])]
     (if (contains? old-props key)
       {:im-chan {:chan (save/delete-bluegenes-properties service (name key))
                  :on-success [:property/success key nil]
                  :on-failure [:property/failure key]}}
       ;; Don't do anything if the key doesn't exist.
       {}))))

(reg-event-db
 :property/success
 (fn [db [_ key value-edn _res]]
   (let [mine-kw (get db :current-mine)
         bg-props (if (nil? value-edn) ; Nil value means we've deleted the key.
                    (dissoc (get-in db [:assets :bg-properties mine-kw])
                            key)
                    (assoc (get-in db [:assets :bg-properties mine-kw])
                           key value-edn))]
     (-> db
         (update-in [:mines mine-kw] merge (bg-properties-to-bluegenes bg-props))
         (assoc-in [:assets :bg-properties mine-kw] bg-props)))))

(reg-event-fx
 :property/failure
 (fn [{db :db} [_ key res]]
   {:dispatch [:messages/add
               {:markup [:span (case key
                                 :layout.report "Failed to save changes to report layout editor. ")
                         (when-let [err (not-empty (get-in res [:body :error]))]
                           [:code err])]
                :style "danger"}]}))

;; TODO handle older mines without this ws
(reg-event-fx
 :assets/fetch-bg-properties
 (fn [{db :db} [evt]]
   (let [current-mine (:current-mine db)
         service      (get-in db [:mines current-mine :service])]
     {:im-chan {:chan (fetch/bluegenes-properties service)
                :on-success [:assets/success-fetch-bg-properties current-mine]
                :on-failure [:assets/failure evt]}})))

(reg-event-db
 :assets/success-fetch-bg-properties
 (fn [db [_ mine-kw bg-properties]]
   (-> db
       ;; We read stuff from our mine.
       (update-in [:mines mine-kw] merge (bg-properties-to-bluegenes bg-properties))
       ;; And compare stuff from our assets.
       (assoc-in [:assets :bg-properties mine-kw] bg-properties))))
