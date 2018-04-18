(ns bluegenes.components.idresolver.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [bluegenes.effects :as fx]
            [oops.core :refer [oget]]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [imcljs.path :as path]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]
            [clojure.string :as string]))

(reg-event-db ::stage-files
              (fn [db [_ js-FileList]]
                (update-in db [:idresolver :stage :files] concat (array-seq js-FileList))))

(reg-event-db ::unstage-file
              (fn [db [_ js-File]]
                (update-in db [:idresolver :stage :files] #(remove (partial = js-File) %))))

(reg-event-db ::update-textbox-identifiers
              (fn [db [_ value]]
                (assoc-in db [:idresolver :stage :textbox] value)))

(reg-event-fx ::parse-staged-files
              (fn [{db :db} [_ js-Files text options]]
                {:db (assoc-in db [:idresolver :stage :status] {:action :parsing})
                 ::fx/http {:uri "/api/ids/parse"
                            :method :post
                            :multipart-params (cond-> (map (fn [f] [(oget f :name) f]) js-Files)
                                                      ; After creating multipart params for files, create one for text
                                                      ; if there's a value
                                                      (not (string/blank? text)) (conj ["text" text])
                                                      ; And also a param for the case sensitive option
                                                      true (conj ["caseSensitive" (:case-sensitive options)]))
                            :on-success [::store-parsed-response options]}}))

(reg-event-fx ::store-parsed-response
              (fn [{db :db} [_ options response]]
                {:db (update db :idresolver #(-> %
                                                 (assoc-in [:stage :status :action] nil)
                                                 (assoc-in [:stage :flags :parsed] true)
                                                 (assoc :to-resolve response)))
                 :dispatch [::resolve-identifiers options (:identifiers response)]}))

(reg-event-fx ::resolve-identifiers
              (fn [{db :db} [_ options identifiers]]
                (let [service (get-in db [:mines (get db :current-mine) :service])]
                  {:db (-> db
                           (assoc-in [:idresolver :stage :view] :review)
                           (assoc-in [:idresolver :stage :options :review-tab] :matches)
                           (update :idresolver dissoc :response))
                   :im-chan {:chan (fetch/resolve-identifiers
                                     service
                                     {:identifiers identifiers
                                      :case-sensitive (:case-sensitive options)
                                      :type (:type options)
                                      :extra (:organism options)})
                             :on-success [::store-identifiers]}
                   :navigate "/upload/save"})))

(def time-formatter (time-format/formatter "dd MMM yyyy HH:mm:ss"))

(reg-event-db ::finished-review
              (fn [db]
                (let [{:keys [type organism]} (get-in db [:idresolver :stage :options])]
                  (-> db
                      (assoc-in [:idresolver :stage :flags] {:reviewed true})
                      (assoc-in [:idresolver :stage :view] :review)
                      (assoc-in [:idresolver :save :list-name]
                                (str type
                                     " list for "
                                     (if (string/blank? organism) "all organisms" organism)
                                     " "
                                     (time-format/unparse time-formatter (time/now))))))))

(reg-event-db ::store-identifiers
              (fn [db [_ response]]
                (let [{:keys [type organism]} (get-in db [:idresolver :stage :options])]
                  (-> db
                      (assoc-in [:idresolver :response] response)
                      (assoc-in [:idresolver :stage :view] :review)
                      (assoc-in [:idresolver :save :list-name]
                                (str type
                                     " list for "
                                     (if (string/blank? organism) "all organisms" organism)
                                     " "
                                     (time-format/unparse time-formatter (time/now))))))))

(reg-event-db ::set-view
              (fn [db [_ view]]
                (assoc-in db [:idresolver :stage :view] view)))

(reg-event-db ::toggle-case-sensitive
              (fn [db [_ response]]
                (update-in db [:idresolver :stage :options :case-sensitive?] not)))



(reg-event-db ::update-type
              (fn [db [_ model value]]
                (let [disable-organism? (when (not= value "_") (not (contains? (path/relationships model value) :organism)))
                      mine-details      (get-in db [:mines (get db :current-mine)])]
                  (if disable-organism?
                    (update-in db [:idresolver :stage :options] assoc :type value :disable-organism? true :organism nil)
                    (update-in db [:idresolver :stage :options] #(cond-> %
                                                                         ; Assoc the new Type value to the options
                                                                         value (assoc :type value)
                                                                         ; Enabled organism dropdown
                                                                         true (assoc :disable-organism? false)
                                                                         ; Give the organism dropdown a value if nil
                                                                         (nil? (:organism %)) (assoc :organism (-> mine-details :default-organism))))))))

(reg-event-db ::update-option
              (fn [db [_ option-kw value]]
                (assoc-in db [:idresolver :stage :options option-kw] value)))


(reg-event-db ::toggle-keep-duplicate
              (fn [db [_ duplicate-idx match-idx]]
                (update-in db [:idresolver :response :matches :DUPLICATE duplicate-idx :matches match-idx :keep?] not)))

(reg-event-db ::update-list-name
              (fn [db [_ value]]
                (assoc-in db [:idresolver :save :list-name] value)))

(reg-event-fx ::save-list
              (fn [{db :db} [_]]
                (let [{:keys [OTHER WILDCARD DUPLICATE TYPE_CONVERTED MATCH]} (get-in db [:idresolver :response :matches])
                      object-type (get-in db [:idresolver :stage :options :type])
                      service     (get-in db [:mines (get db :current-mine) :service])
                      list-name   (get-in db [:idresolver :save :list-name])]
                  {:im-chan {:chan (save/im-list-from-query
                                     service
                                     list-name
                                     {:from object-type
                                      :select [(str object-type ".id")]
                                      :where [{:path (str object-type ".id")
                                               :op "ONE OF"
                                               :values (->> MATCH
                                                            (concat (mapcat :matches OTHER))
                                                            (concat (mapcat :matches TYPE_CONVERTED))
                                                            (concat (mapcat (fn [{matches :matches}] (filter :keep? matches)) DUPLICATE))
                                                            (map :id))}]})
                             :on-success [::save-list-success list-name object-type]}})))

(reg-event-fx ::save-list-success
              (fn [{db :db} [_ list-name object-type response]]
                ; Get the summary fields for this object type (used to construct the query for the List Analysis page
                (let [{:keys [listName]} response
                      summary-fields (get-in db [:assets :summary-fields (get db :current-mine) (keyword object-type)])]
                  {:db db
                   ; Navigate to the List Analysis page
                   ;:navigate "results"
                   :dispatch-n [
                                ; Re-fetch our lists so that it shows in MyMine
                                [:assets/fetch-lists]
                                ; Show the list results in the List Analysis page
                                [:results/history+ {:source (get db :current-mine)
                                                    :type :query
                                                    :value {:title list-name
                                                            :from object-type
                                                            :select summary-fields
                                                            :where [{:path object-type
                                                                     :op "IN"
                                                                     :value list-name}]}}]]})))

(reg-event-db ::reset
              (fn [db [_ example-type example-text]]
                (let [mine-details (get-in db [:mines (get db :current-mine)])]
                  (assoc db :idresolver
                            {:stage {:files nil
                                     :textbox example-text
                                     :options {:case-sensitive false
                                               :type (or example-type (-> mine-details :default-object-types first name))
                                               :organism (-> mine-details :default-organism)}
                                     :status nil
                                     :flags nil}
                             :response nil}))))

(reg-event-fx ::load-example
              (fn [{db :db} [_]]
                (let [[t e] (first (seq (get-in db [:mines (get db :current-mine) :idresolver-example])))]
                  {:dispatch [::reset (name t) e]})))