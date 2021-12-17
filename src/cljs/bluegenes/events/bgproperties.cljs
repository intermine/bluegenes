(ns bluegenes.events.bgproperties
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [cljs.reader :as reader]
            [bluegenes.pages.admin.events :refer [import-categories]]
            [bluegenes.utils :refer [compatible-version?]]))

(defn read-prop [value]
  (reader/read-string value))

(defn write-prop [value]
  (pr-str value))

(defn nil-assert
  "Assert that a predicate returns truthy for x, returning x if it does and nil otherwise."
  [x pred]
  (if (pred x)
    x
    nil))

(defn bg-properties-to-bluegenes
  "Returns a map generated from `bg-properties` to be merged with the current
  mine. Note that if a property is missing, its value will be nil. This is to
  ensure that deleted keys get their previous value overwritten with nil."
  [bg-properties]
  {:report-layout (some-> (get bg-properties :layout.report) read-prop (nil-assert map?) import-categories)
   :notice (some-> (get bg-properties :notice) read-prop (nil-assert string?))})

;; Note that making changes to bluegenes-properties will fail on the InterMine
;; backend if you're not authenticated as an admin.

(reg-event-fx
 :property/save
 (fn [{db :db} [_ key value {:keys [on-success on-failure]}]]
   (let [mine-kw (get db :current-mine)
         old-props (get-in db [:assets :bg-properties mine-kw])
         service (get-in db [:mines mine-kw :service])
         value-edn (write-prop value)
         current-version (get-in db [:assets :intermine-version (:current-mine db)])]
     (cond
       (and (contains? old-props key)
            (= (get old-props key) value-edn))
       {:dispatch on-success} ; No changes made; do nothing!

       (contains? old-props key)
       {:im-chan {:chan (save/update-bluegenes-properties service (name key) value-edn
                                                          ;; This tells imcljs to use :put instead of :put-body, as the latter isn't supported until 5.0.4.
                                                          nil (when-not (compatible-version? "5.0.4" current-version) true))
                  :on-success [:property/success key value-edn on-success]
                  :on-failure on-failure}}

       :else
       {:im-chan {:chan (save/bluegenes-properties service (name key) value-edn)
                  :on-success [:property/success key value-edn on-success]
                  :on-failure on-failure}}))))

(reg-event-fx
 :property/delete
 (fn [{db :db} [_ key {:keys [on-success on-failure]}]]
   (let [mine-kw (get db :current-mine)
         old-props (get-in db [:assets :bg-properties mine-kw])
         service (get-in db [:mines mine-kw :service])]
     (if (contains? old-props key)
       {:im-chan {:chan (save/delete-bluegenes-properties service (name key))
                  :on-success [:property/success key nil on-success]
                  :on-failure on-failure}}
       ;; Don't do anything if the key doesn't exist.
       {:dispatch on-success}))))

(reg-event-fx
 :property/success
 (fn [{db :db} [_ key value-edn on-success _res]]
   (let [mine-kw (get db :current-mine)
         bg-props (if (nil? value-edn) ; Nil value means we've deleted the key.
                    (dissoc (get-in db [:assets :bg-properties mine-kw])
                            key)
                    (assoc (get-in db [:assets :bg-properties mine-kw])
                           key value-edn))]
     {:db (-> db
              (update-in [:mines mine-kw] merge (bg-properties-to-bluegenes bg-props))
              (assoc-in [:assets :bg-properties mine-kw] bg-props))
      :dispatch on-success})))

(reg-event-fx
 :assets/fetch-bg-properties
 (fn [{db :db} [evt]]
   (let [current-mine (:current-mine db)
         service      (get-in db [:mines current-mine :service])]
     {:im-chan {:chan (fetch/bluegenes-properties service)
                :on-success [:assets/success-fetch-bg-properties current-mine]
                ;; Older mines don't have this webservice, so no errors, no worries!
                :on-failure [:assets/success-fetch-bg-properties current-mine]}})))

;; The below effect reassures any mine admin paying attention to the CORS
;; errors in the browser console. Older mines without `/bluegenes-properties`
;; respond with a 302 redirect to `Location /{mine}/begin.do;jsessionid=XXXX`.
;; Luckily the only side-effect of this is the logged CORS and failed requests.
(reg-fx
 ::bg-properties-missing
 (fn [{:keys [mine-kw]}]
   (.warn js/console
          (str "BlueGenes Warning: " (name mine-kw) " is running an older InterMine version without the `/bluegenes-properties` web service. Some features may be missing (more details are displayed on the affected pages). You may see CORS errors and failed requests in the browser console, but these can be safely ignored."))))

(reg-event-fx
 :assets/success-fetch-bg-properties
 (fn [{db :db} [_ mine-kw bg-properties]]
   (if bg-properties ; Nil when webservice isn't present.
     {:db (-> db
              ;; We read stuff from our mine.
              (update-in [:mines mine-kw] merge (bg-properties-to-bluegenes bg-properties))
              ;; And compare stuff from our assets.
              (assoc-in [:assets :bg-properties mine-kw] bg-properties))}
     {::bg-properties-missing {:mine-kw mine-kw}})))
