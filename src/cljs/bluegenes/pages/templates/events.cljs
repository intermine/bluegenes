(ns bluegenes.pages.templates.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch subscribe]]
            [re-frame.events]
            [oops.core :refer [oset!]]
            [goog.dom :as gdom]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [imcljs.fetch :as fetch]
            [imcljs.save :as save]
            [bluegenes.route :as route]
            [bluegenes.components.ui.constraint :as constraint]
            [bluegenes.pages.templates.helpers :refer [prepare-template-query template-matches?]]
            [bluegenes.utils :refer [template->xml]]))

(defn template-matches-filters? [db template-id]
  (let [{:keys [selected-template-category text-filter authorized-filter]}
        (get-in db [:components :template-chooser])
        template (get-in db [:assets :templates (:current-mine db) template-id])]
    (template-matches? {:category selected-template-category
                        :text text-filter
                        :authorized authorized-filter}
                       template)))

;; This effect handler is used from routes and has different behaviour
;; depending on if it's called from a different panel, or the template panel.
(reg-event-fx
 :template-chooser/open-template
 (fn [{db :db} [_ id]]
   (if (= :templates-panel (:active-panel db))
     {:dispatch [:template-chooser/choose-template id]}
     {:dispatch-n [[:set-active-panel :templates-panel
                    nil
                    ;; flush-dom makes the event wait for the page to update first.
                    ;; This is because we'll be scrolling to the template, so the
                    ;; element needs to be present first.
                    ^:flush-dom [:template-chooser/choose-template id
                                 {:scroll? true}]]]})))

(reg-event-fx
 :template-chooser/choose-template
 (fn [{db :db} [_ id {:keys [scroll?] :as _opts}]]
   (let [matches-filters? (template-matches-filters? db id)
         current-mine (:current-mine db)
         query (get-in db [:assets :templates current-mine id])]
     (cond
       (and matches-filters?
            (not-empty query)) (merge
                                {:db (update-in db [:components :template-chooser] assoc
                                                :selected-template query
                                                :selected-template-name id
                                                :selected-template-service (get-in db [:mines current-mine :service])
                                                :count nil
                                                :results-preview nil)
                                 :dispatch-n [[:template-chooser/run-count]
                                              [:template-chooser/fetch-preview]]}
                                (when scroll?
                                  {:scroll-to-template {:id (name id)}}))

       ;; Clear filters by using assoc-in instead of update-in assoc.
       (not-empty query) {:db (assoc-in db [:components :template-chooser]
                                        {:selected-template query
                                         :selected-template-name id
                                         :selected-template-service (get-in db [:mines current-mine :service])
                                         :count nil
                                         :results-preview nil})
                          :dispatch-n [[:template-chooser/run-count]
                                       [:template-chooser/fetch-preview]]
                          ;; Will always scroll as this clause means the user cannot see the
                          ;; template and likely chose it using the browser's back/forward.
                          :scroll-to-template {:id (name id)
                                               :delay 100}}

       ;; Template can't be found.
       :else {:dispatch-n [[::route/navigate ::route/templates]
                           [:messages/add
                            {:markup [:span "The template " [:em (name id)] " does not exist. It's possible the ID has changed. Use the text filter above to find a template with a similar name."]
                             :style "warning"
                             :timeout 0}]]}))))

(reg-event-db
 :template-chooser/deselect-template
 (fn [db [_]]
   (update-in db [:components :template-chooser] select-keys
              [:selected-template-category :text-filter :authorized-filter])))
;; Above keeps filters, while the below clears them.
(reg-event-db
 :template-chooser/clear-template
 (fn [db [_]]
   (update db :components dissoc :template-chooser)))

(reg-event-db
 :template-chooser/set-category-filter
 (fn [db [_ id]]
   (assoc-in db [:components :template-chooser :selected-template-category] id)))

(reg-event-db
 :template-chooser/set-text-filter
 (fn [db [_ text]]
   (assoc-in db [:components :template-chooser :text-filter] text)))

(reg-event-db
 :template-chooser/toggle-authorized-filter
 (fn [db [_]]
   (update-in db [:components :template-chooser :authorized-filter] not)))

;; We don't want to make the text filter a controlled input as we want to be
;; able to debounce its event. Leading to this lesser evil of DOM manipulation.
(reg-fx
 ::clear-text-filter
 (fn [_]
   (oset! (gdom/getElement "template-text-filter") :value "")))

(reg-event-fx
 :template-chooser/clear-text-filter
 (fn [{db :db} [_]]
   {:db (assoc-in db [:components :template-chooser :text-filter] "")
    ::clear-text-filter {}}))

(reg-event-fx
 :templates/send-off-query
 (fn [{db :db} [_]]
   {:db db
    :dispatch [:results/history+
               {:source (:current-mine db)
                :type :query
                :intent :template
                :value (prepare-template-query (get-in db [:components :template-chooser :selected-template]))}]}))

(reg-event-db
 :templates/reset-template
 (fn [db [_]]
   (let [current-mine (:current-mine db)
         id (get-in db [:components :template-chooser :selected-template-name])]
     (assoc-in db [:components :template-chooser :selected-template]
               (get-in db [:assets :templates current-mine id])))))

(reg-event-fx
 :templates/edit-query
 (fn [{db :db} [_]]
   {:db db
    :dispatch-n [[::route/navigate ::route/querybuilder]
                 [:qb/load-query (prepare-template-query (get-in db [:components :template-chooser :selected-template]))]]}))

(reg-event-fx
 :templates/edit-template
 (fn [{db :db} [_]]
   {:db db
    :dispatch-n [[::route/navigate ::route/querybuilder]
                 [:qb/load-template (get-in db [:components :template-chooser :selected-template])]]}))

(reg-event-fx
 :template-chooser/replace-constraint
 (fn [{db :db} [_ index {new-op :op :as new-constraint}]]
   (let [constraint-location [:components :template-chooser :selected-template :where index]
         {old-op :op :as old-constraint} (get-in db constraint-location)]
     {:db (-> db
              (assoc-in constraint-location (constraint/clear-constraint-value old-constraint new-constraint))
              (cond->
                (not= old-op new-op) (assoc-in [:components :template-chooser :results-preview] nil)))})))

(reg-event-fx
 :template-chooser/update-preview
 (fn [{db :db} [_ _index new-constraint]]
   (if (constraint/satisfied-constraint? new-constraint)
     {:dispatch-n [[:template-chooser/run-count]
                   [:template-chooser/fetch-preview]]}
     {:db (assoc-in db [:components :template-chooser :results-preview] nil)})))

(reg-event-db
 :template-chooser/update-count
 (fn [db [_ c]]
   (update-in db [:components :template-chooser] assoc
              :count c
              :counting? false)))

(reg-event-db
 :template-chooser/store-results-preview
 (fn [db [_ results]]
   (update-in db [:components :template-chooser] assoc
              :results-preview results
              :fetching-preview? false)))

(reg-fx
 :template-chooser/pipe-preview
 (fn [preview-chan]
   (go (dispatch [:template-chooser/store-results-preview (<! preview-chan)]))))

(reg-event-fx
 :template-chooser/fetch-preview
 (fn [{db :db}]
   (let [query (prepare-template-query (get-in db [:components :template-chooser :selected-template]))
         service (get-in db [:mines (:current-mine db) :service])
         count-chan (fetch/table-rows service query {:size 5})
         query-changed? (not= query (get-in db [:components :template-chooser :previously-ran]))
         new-db (update-in db [:components :template-chooser] assoc
                           :preview-chan count-chan
                           :previously-ran query)]
     (cond-> {:db new-db}
       query-changed? (-> (update-in [:db :components :template-chooser] assoc
                                     :fetching-preview? true
                                     :preview-error nil)
                          (assoc :im-chan {:chan count-chan
                                           :on-success [:template-chooser/store-results-preview]
                                           :on-failure [:template-chooser/fetch-preview-failure]}))))))

(reg-event-db
 :template-chooser/fetch-preview-failure
 (fn [db [_ res]]
   (update-in db [:components :template-chooser] assoc
              :fetching-preview? false
              :preview-error (or (get-in res [:body :error])
                                 "Error occurred when running template."))))

(reg-fx
 :template-chooser/pipe-count
 (fn [count-chan]
   (go (dispatch [:template-chooser/update-count (<! count-chan)]))))

(reg-event-fx
 :template-chooser/run-count
 (fn [{db :db}]
   (let [query (prepare-template-query (get-in db [:components :template-chooser :selected-template]))
         service (get-in db [:mines (:current-mine db) :service])
         count-chan (fetch/row-count service query)
         new-db (update-in db [:components :template-chooser] assoc
                           :count-chan count-chan
                           :counting? true)]
     {:db new-db
      :template-chooser/pipe-count count-chan})))

(reg-event-fx
 :templates/delete-template
 (fn [{db :db} [_]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         template-name (name (get-in db [:components :template-chooser :selected-template-name]))
         template-details (get-in db [:components :template-chooser :selected-template])]
     {:im-chan {:chan (save/delete-template service template-name)
                :on-success [:templates/delete-template-success template-name template-details]
                :on-failure [:templates/delete-template-failure template-name template-details]}})))

(reg-event-fx
 :templates/delete-template-success
 (fn [{db :db} [_ template-name template-details _res]]
   {:db (update-in db [:assets :templates (:current-mine db)]
                   dissoc (keyword template-name))
    :dispatch-n [[::route/navigate ::route/templates]
                 [:messages/add
                  {:markup (fn [id]
                             [:span
                              "The template "
                              [:em template-name]
                              " has been deleted. "
                              [:a {:role "button"
                                   :on-click #(dispatch [:templates/undo-delete-template
                                                         template-name template-details id])}
                               "Click here"]
                              " to undo this action and restore the template."])
                   :style "info"
                   :timeout 10000}]]}))

(reg-event-fx
 :templates/undo-delete-template
 (fn [{db :db} [_ template-name template-details message-id]]
   (let [service (get-in db [:mines (:current-mine db) :service])
         model (:model service)
         ;; We're passing template-details as the query here. It's fine as it
         ;; contains the expected query keys, and imcljs.query/->xml will only
         ;; use the query-related keys, ignoring the rest.
         template-query (template->xml model template-details template-details)]
     {:im-chan {:chan (save/template service template-query)
                :on-success [:templates/undo-delete-template-success template-name]
                :on-failure [:qb/save-template-failure template-name]}
      :dispatch [:messages/remove message-id]})))

(reg-event-fx
 :templates/undo-delete-template-success
 (fn [{db :db} [_ template-name _res]]
   {:dispatch [:assets/fetch-templates
               [::route/navigate ::route/template {:template template-name}]]}))

(reg-event-fx
 :templates/delete-template-failure
 (fn [{db :db} [_ template-name _template-details res]]
   {:dispatch [:messages/add
               {:markup [:span (str "Failed to delete template '" template-name "'. "
                                    (or (get-in res [:body :error])
                                        "Please check your connection and try again."))]
                :style "warning"}]}))
