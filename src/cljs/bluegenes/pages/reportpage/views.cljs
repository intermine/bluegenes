(ns bluegenes.pages.reportpage.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.pages.reportpage.components.summary :as summary]
            [bluegenes.pages.reportpage.components.table :as report-table]
            [bluegenes.pages.reportpage.components.toc :as toc]
            [bluegenes.pages.reportpage.components.sidebar :as sidebar]
            [bluegenes.pages.reportpage.utils :as utils]
            #_[bluegenes.components.table :as table]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.components.tools.views :as tools]
            [bluegenes.pages.reportpage.subs :as subs]
            [im-tables.views.core :as im-table]
            [bluegenes.route :as route]
            [bluegenes.components.viz.views :as viz]
            [bluegenes.components.icons :refer [icon]]))

(defn tbl [{:keys [loc collapse]}]
  (let [data (subscribe [::subs/a-table loc])
        is-collapsed* (r/atom collapse)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [{:keys [loc] :as data} (r/props this)]
                               (when-not @is-collapsed*
                                 (dispatch [:im-tables/load loc (dissoc data :loc)]))))
      :reagent-render (fn [{:keys [loc title]}]
                        (fn []
                          (let [result-count (get-in @data [:response :iTotalRecords])]
                            [:div.report-item
                             {:class (when @is-collapsed* :report-item-collapsed)}
                             [:h4.report-item-heading
                              {:on-click #(swap! is-collapsed* not)}
                              (str title (when result-count (str " (" result-count ")")))
                              [:span.report-item-toggle
                               (if @is-collapsed*
                                 [icon "expand-folder"]
                                 [icon "collapse-folder"])]]
                             (cond
                               @is-collapsed* nil
                               (= 0 result-count) [:div "No Results"]
                               :else [:div {:style {:background-color "white"}}
                                      [im-table/main loc]])])))})))

(defn ->report-table-settings [current-mine-name]
  {:pagination {:limit 5}
   :links {:vocab {:mine (name (or current-mine-name ""))}
           :url (fn [{:keys [mine class objectId] :as vocab}]
                  (route/href ::route/report
                              {:mine mine
                               :type class
                               :id objectId}))}})

(defn tool-report [child]
  [:p child])

(defn template-report [{:keys [id collapse] template-name :value}]
  (let [summary-fields @(subscribe [:current-summary-fields])
        service (:service @(subscribe [:current-mine]))
        current-mine-name @(subscribe [:current-mine-name])
        {:keys [title] nom :name :as template} @(subscribe [::subs/a-template template-name])]
    [tbl {:loc [:report-page id nom]
          :service (merge service {:summary-fields summary-fields})
          :title title
          :collapse collapse
          :query template
          :settings (->report-table-settings current-mine-name)}]))

(defn class-report [{:keys [id collapse] referencedType :value}]
  (let [{object-type :type object-id :id} @(subscribe [:panel-params])
        summary-fields @(subscribe [:current-summary-fields])
        service (:service @(subscribe [:current-mine]))
        current-mine-name @(subscribe [:current-mine-name])
        {:keys [displayName] nom :name :as ref+coll} @(subscribe [::subs/a-ref+coll referencedType])]
    [tbl {:loc [:report-page id nom]
          :service (merge service {:summary-fields summary-fields})
          :title displayName
          :collapse collapse
          :query (utils/->query-ref+coll summary-fields object-type object-id ref+coll)
          :settings (->report-table-settings current-mine-name)}]))

(defn report []
  (let [categories @(subscribe [:bluegenes.pages.admin.subs/categories])]
    [:div
     (for [{:keys [category id children]} categories
           :when (seq children)] ; No point having a section without children.
       ^{:key id}
       [report-table/section
        {:title category}
        [:div
         (for [{:keys [label value type collapse id] :as child} children
               :let [report-comp (case type
                                   "class" class-report
                                   "template" template-report
                                   "tool" tool-report)]]
           ^{:key id}
           [report-comp child])]])]))

(defn heading []
  [:h1 "BRCA1"
   [:code.start {:class (str "start-" "Gene")} "Gene"]])

(defn main []
  [:div.container-fluid.report-page
   [:div.row.report-row
    [:div.col-xs-2
     [toc/main]]
    [:div.col-xs-8
     [heading]
     [report-table/summary]
     [report]]
    [:div.col-xs-2
     [sidebar/main]]]])
