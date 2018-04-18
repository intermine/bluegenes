(ns bluegenes.components.idresolver.events-inplace
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [bluegenes.db :as db]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [accountant.core :refer [navigate!]]
            [clojure.zip :as zip]))

(def ns->kw (comp keyword namespace))

(defn get-object-type
  "returns either the currently selected object-type or the default if none has been selected"
  [db]
  (let [object-type         (get-in db [:idresolver :selected-object-type])
        object-type-default (get-in db [:mines (get db :current-mine) :default-selected-object-type])]
    (if (some? object-type)
      object-type
      object-type-default)))

(defn get-organism-type
  "returns either the currently selected organism or the default if none has been selected"
  [db]
  (let [organism (get-in db [:idresolver :selected-organism :shortName])]
    (if (some? organism)
      organism
      :any)
    ))

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(reg-event-fx
  :handle-id
  (fn [{db :db} [_ id]]
    {:db (let [{{:keys [MATCH TYPE_CONVERTED OTHER WILDCARD DUPLICATE] :as resolved} :matches
                unresolved :unresolved} id
               tagged    (remove empty? (mapcat (fn [[k v]] (map (fn [r] (assoc r :status k)) v)) resolved))
               tagged-un (reduce (fn [total next] (assoc total next {:input [next] :status :UNRESOLVED})) {} unresolved)]

           (-> db
               (update-in [:idresolver :results]
                          (fn [results]
                            (merge tagged-un
                                   (reduce (fn [total next-id]
                                             (merge total
                                                    (reduce (fn [n next-input]
                                                              (assoc n next-input next-id)) {}
                                                            (if (vector? (:input next-id))
                                                              (:input next-id)
                                                              [(:input next-id)]))))
                                           results tagged))))))
     :dispatch [:idresolver/analyse false]}))

(reg-fx
  :idresolver/resolve-id
  (fn [[ids service db]]
    (let [organism    (get-organism-type db)
          object-type (get-object-type db)
          job         (fetch/resolve-identifiers service
                                                 (cond->
                                                   {:identifiers (if (seq ids) ids [ids])
                                                    :type object-type}
                                                   (not= organism :any) (assoc :extra organism)))]
      (go (dispatch [:handle-id (<! job)])))))

(reg-event-fx
  :idresolver/resolve
  (fn [{db :db} [_ ids]]
    (let [service (get-in db [:mines (get db :current-mine) :service])]
      {:db
       (-> db
           (assoc-in [:idresolver :resolving?] true)
           (update-in [:idresolver :bank]
                      (fn [bank]
                        (distinct (reduce (fn [total next]
                                            (conj total {:input next
                                                         :status :inactive})) bank ids)))))
       :idresolver/resolve-id
       [ids service db]})))

(defn toggle-into-collection [coll val]
  (if-let [found (some #{val} coll)]
    (remove #(= % found) coll)
    (conj coll val)))

(reg-event-db
  :idresolver/toggle-selected
  (fn [db [_ id]]
    (let [multi-select? (get-in db [:idresolver :select-multi])]
      (if multi-select?
        (update-in db [:idresolver :selected] toggle-into-collection id)
        (if (= id (first (get-in db [:idresolver :selected])))
          (assoc-in db [:idresolver :selected] [])
          (assoc-in db [:idresolver :selected] [id]))))))

(reg-event-db
  :idresolver/remove-from-bank
  (fn [db [_ selected]]
    (assoc-in db [:idresolver :bank]
              (reduce (fn [total next]
                        (if-not (some? (some #{(:input next)} selected))
                          (conj total next)
                          total)) [] (get-in db [:idresolver :bank])))))

(reg-event-db
  :idresolver/remove-from-results
  (fn [db [_ selected]]
    (update-in db [:idresolver :results] (partial apply dissoc) selected)))

(reg-event-fx
  :idresolver/delete-selected
  (fn [{db :db}]
    (let [selected (get-in db [:idresolver :selected])]
      {:db (assoc-in db [:idresolver :selected] '())
       :dispatch-n [[:idresolver/remove-from-bank selected]
                    [:idresolver/remove-from-results selected]]})))

(reg-event-db
  :idresolver/clear-selected
  (fn [db]
    (assoc-in db [:idresolver :selected] '())))

(reg-event-db
  :idresolver/clear
  (fn [db]
    (update-in db [:idresolver] assoc
               :bank nil
               :results nil
               :resolving? false
               :selected '())))

(reg-event-db
  :idresolver/toggle-select-multi
  (fn [db [_ tf]]
    (assoc-in db [:idresolver :select-multi] tf)))

(reg-event-db
  :idresolver/toggle-select-range
  (fn [db [_ tf]]
    (assoc-in db [:idresolver :select-range] tf)))

(defn pull-inputs-from-id-resolver [db]
  (let [bank (get-in db [:idresolver :bank])]
    (reduce (fn [new-idlist id] (conj new-idlist (:input id))) [] bank)
    ))


(reg-event-fx
  :idresolver/set-selected-organism
  (fn [{db :db} [_ organism]]

    (let [ids (pull-inputs-from-id-resolver db)]
      {:db (-> db
               (assoc-in [:idresolver :selected-organism] organism)
               (assoc-in [:idresolver :bank] nil)
               (assoc-in [:idresolver :results] nil))
       :dispatch [:idresolver/resolve ids]})))

(reg-event-fx
  :idresolver/set-selected-object-type
  (fn [{db :db} [_ object-type]]
    (let [service (get-in db [:mines (:current-mine db) :service])
          ids     (pull-inputs-from-id-resolver db)]
      {:db (-> db
               (assoc-in [:idresolver :selected-object-type] object-type)
               (assoc-in [:idresolver :bank] nil))
       :dispatch [:idresolver/resolve ids]})))

(reg-event-fx
  ;;TODO:    This is probably an obsolete method. If we do res it,
  ;;         make sure it's generic and not gene-specific.
  :idresolver/save-results
  (fn [{db :db}]
    (let [ids     (remove nil? (map (fn [[_ {id :id}]] id) (-> db :idresolver :results)))
          results {:sd/type :query
                   :sd/service :flymine
                   :sd/label (str "Uploaded " (count ids) " Genes")
                   :sd/value {:from "Gene"
                              :title (str "Uploaded " (count ids) " Genes")
                              :select (get-in db [:assets :summary-fields :Gene])
                              :where [{:path "Gene.id"
                                       :op "ONE OF"
                                       :values ids}]}}]
      {:db db
       :dispatch [:save-data results]
       ;:navigate "saved-data"
       })))


(defn pull-ids-from-idresolver
  "Returns IDs from the idresolver data set. straight matches have an id at a lower level than the converted and duplicate types so we need to do some deep digging. Right now if the user doesn't choose an option for the duplicate, we automatically serve up the first one in all future lists."
  [results]
  (remove nil? (map
                 (fn [[_ {id :id matches :matches}]]
                   (if (some? id)
                     id
                     (:id (first matches))))
                 results))
  )

(defn build-query
  "Builds the structure for the preview results query, given a set of successfully identified IDs"
  [ids object-type summary-fields]
  (let [object-type (name object-type)
        plural?     (> (count ids) 1)
        ;this pluralisation works for proteins and genes. one day we can get fancy
        ; and worry about -ies and other pluralisations, but today is not that day.
        label       (str "Uploaded " (count ids) " " object-type (cond plural? "s"))]
    {:type :query
     :label label
     :value {:title label
             :from object-type
             :select summary-fields
             :where [{:path (str object-type ".id")
                      :op "ONE OF"
                      :values ids}]}}))

(reg-event-fx
  :idresolver/analyse
  (fn [{db :db} [_ navigate?]]
    (let [uid            (str (gensym))
          ids            (pull-ids-from-idresolver (-> db :idresolver :results))
          current-mine   (:current-mine db)
          object-type    (get-object-type db)
          summary-fields (get-in db [:assets :summary-fields current-mine object-type])
          results        (build-query ids object-type summary-fields)]
      {:dispatch-n [[:results/history+ {:source (get db :current-mine)
                                        :type :query
                                        :value (:value results)}]
                    [:idresolver/fetch-preview results]]})))



(reg-event-db
  :idresolver/resolve-duplicate
  (fn [db [_ input result]]
    (let [symbol (:symbol (:summary result))]
      (-> db
          ; Associate the chosen result to the results map using its symbol
          (assoc-in [:idresolver :results symbol]
                    (assoc result
                      :input [symbol]
                      :status :MATCH))
          ; Dissociate the old {:input {...}} result
          (dissoc-in [:idresolver :results input])
          ; Replace the old input with the new input
          (update-in [:idresolver :bank]
                     (fn [bank]
                       (map (fn [next]
                              (if (= input (:input next))
                                {:input symbol
                                 :status :inactive}
                                next)) bank)))))))

(reg-event-db
  :idresolver/store-results-preview
  (fn [db [_ results]]
    (update-in db [:idresolver] assoc
               :results-preview results
               :fetching-preview? false)))

(reg-fx
  :idresolver/pipe-preview
  (fn [preview-chan]
    (go (dispatch [:idresolver/store-results-preview (<! preview-chan)]))))

(reg-event-fx
  :idresolver/fetch-preview
  (fn [{db :db} [_ query]]
    (let [mine       (:current-mine db)
          service    (get-in db [:mines mine :service])
          count-chan (fetch/table-rows service (:value query) {:size 5})
          new-db     (update-in db [:idresolver] assoc
                                :preview-chan count-chan
                                :fetching-preview? true)]
      {:db new-db
       :im-chan {:chan count-chan
                 :on-success [:idresolver/store-results-preview]}})))


(defn get-default-example
  "Fallback for scenarios where soem one's using the id resolver nd it's not well configured"
  [db]
  (let [current-mine         (get-in db [:mines (:current-mine db)])
        ;pull the default from the db
        object-type-default  (:default-selected-object-type current-mine)
        examples             (get-in current-mine [:idresolver-example])
        example-text-default (object-type-default examples)
        ;can we use the default?
        use-default?         (some? example-text-default)
        ;grab any example there is, in case there isn't a default.
        any-example-type     (first (keys examples))
        any-example-text     (any-example-type examples)
        ;where we store the data
        example-keys         [:idresolver :selected-object-type]]
    {:db (cond->
           ;sets example to any type configured as a fallback
           (assoc-in db example-keys any-example-type)
           ;if there's a default type, set the example to it instead.
           use-default? (assoc-in example-keys object-type-default))
     :text (if use-default? example-text-default any-example-text)}))

(reg-event-fx
  :idresolver/example
  (fn [{db :db} [_ splitter]]
    (let [current-mine (get-in db [:mines (:current-mine db)])
          object-type  (get-object-type db)
          ;reset entries to the upload when we choose an example.
          cleared-db   (assoc-in db [:idresolver :bank] [])
          examples     (get-in current-mine [:idresolver-example])
          example-text (object-type examples)]
      ; attempt to show an example from the currently selected object type.
      ; If theres is no example configured, we'll try to fall back to a default.
      (if (some? example-text)
        {:db cleared-db
         :dispatch [:idresolver/resolve (splitter example-text)]}
        (let [fallback-example (get-default-example cleared-db)]
          {:db (:db fallback-example)
           :dispatch [:idresolver/resolve (splitter (:text fallback-example))]}
          ))
      )))
