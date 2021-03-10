(ns bluegenes.pages.home.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [re-frame.std-interceptors :refer [path]]
            [imcljs.save :as im-save]
            [clojure.string :as str]
            [goog.style :as gstyle]
            [goog.dom :as gdom]))

(def root [:home])

(reg-event-db
 :home/clear
 (path root)
 (fn [home [_]]
   (dissoc home :feedback-response)))

(reg-event-db
 :home/select-template-category
 (path root)
 (fn [home [_ category]]
   (assoc home :active-template-category category)))

(reg-event-db
 :home/select-mine-neighbourhood
 (path root)
 (fn [home [_ neighbourhood]]
   (assoc home :active-mine-neighbourhood neighbourhood)))

(reg-event-db
 :home/select-preview-mine
 (path root)
 (fn [home [_ mine-ns]]
   (assoc home :active-preview-mine mine-ns)))

(reg-event-fx
 :home/query-data-sources
 (fn [{db :db} [_]]
   {:dispatch [:results/history+
               {:source (:current-mine db)
                :type :query
                :intent :predefined
                :value {:from "DataSet"
                        :select ["DataSet.name"
                                 "DataSet.url"
                                 "DataSet.dataSource.name"
                                 "DataSet.publication.title"]
                        :constraintLogic nil
                        :where []
                        :sortOrder []
                        :joins ["DataSet.dataSource" "DataSet.publication"]
                        :title "All data sources"}}]}))

(reg-event-fx
 :home/submit-feedback
 (fn [{db :db} [_ email feedback]]
   (let [service (get-in db [:mines (get db :current-mine) :service])]
     (if (str/blank? feedback)
       {:db (assoc-in db (concat root [:feedback-response]) {:type :failure
                                                             :message "Feedback text can't be left blank."})}
       {:im-chan {:chan (im-save/feedback service email feedback)
                  :on-success [:home/feedback-success]
                  :on-failure [:home/feedback-failure]}}))))

(reg-event-db
 :home/feedback-success
 (path root)
 (fn [home [_ _res]]
   (assoc home :feedback-response {:type :success})))

(reg-event-db
 :home/feedback-failure
 (path root)
 (fn [home [_ res]]
   (assoc home :feedback-response {:type :failure
                                   :message "Failed to submit feedback. Alternatively, you can use the email icon in the footer at the bottom of the page. "
                                   :error (get-in res [:body :error])})))

(reg-event-fx
 :home/scroll-to-feedback
 (fn [_ [_]]
   {::scroll-to-feedback {}}))

(reg-fx
 ::scroll-to-feedback
 (fn [_]
   (gstyle/scrollIntoContainerView (gdom/getElement "feedbackform") nil true)))
