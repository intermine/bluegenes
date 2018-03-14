(ns bluegenes.sections.reportpage.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.sections.reportpage.components.summary :as summary]
            [bluegenes.components.table :as table]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.sections.reportpage.components.minelinks :as minelinks]
            [accountant.core :refer [navigate!]]
            [bluegenes.sections.reportpage.subs :as subs]
            [im-tables.views.core :as im-table]
            [imcljs.path :as im-path]))


(defn tbl [{:keys [loc]}]
  (let [data (subscribe [::subs/a-table loc])]
    (r/create-class
      {:component-did-mount (fn [this]
                              (let [{:keys [loc path constraint] :as data} (r/props this)]
                                (dispatch [:im-tables/load loc (dissoc data :loc)])))
       :reagent-render (fn [{:keys [loc title all]}]
                         (fn []
                           (let [result-count (get-in @data [:response :iTotalRecords])]
                             [:div
                              [:h3 (str title (when result-count (str " (" result-count ")")))]
                              (if (= 0 result-count)
                                [:div "No Results"]
                                [:div {:style {:background-color "white"}}
                                 [im-table/main loc]])])))})))

(defn strip-class [s]
  (clojure.string/join "." (drop 1 (clojure.string/split s "."))))

(def clj-name name)

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])
        categories (subscribe [:template-chooser-categories])
        templates (subscribe [:runnable-templates])
        service (subscribe [:current-mine])
        model (subscribe [:current-model])
        current-mine-name (subscribe [:current-mine-name])
        fetching-report? (subscribe [:fetching-report?])
        summary-fields (subscribe [:current-summary-fields])
        runnable-templates (subscribe [::subs/runnable-templates])]
    (fn []
      [:div.container.report
       (let [
             ; TODO Move the following heavy lifting to the events and subs:
             collections (vals (get-in @model [:classes (keyword (:type @params)) :collections]))
             references (vals (get-in @model [:classes (keyword (:type @params)) :references]))
             non-empty-collections (filter (fn [c] (> (get-in @model [:classes (keyword (:referencedType c)) :count]) 0)) collections)
             non-empty-references (filter (fn [c] (> (get-in @model [:classes (keyword (:referencedType c)) :count]) 0)) references)]

         (let [{:keys [type id]} @params]
           [:div
            (if @fetching-report?
              [loader (str (:type @params) " Report")])
            [summary/main (:summary @report)]
            (cond (= "Gene" (:type @params))
                  [minelinks/main (:id @params)])

            ; Only show the body of the report when the summary has loaded
            (when (:summary @report)
              [:div.report-body
               (into [:div]
                     (map (fn [{:keys [name title] :as t}]
                            (let [key nil]
                              [tbl {:loc [:report-page id name]
                                    :service (:service @service)
                                    :title title
                                    :query t
                                    :settings {:pagination {:limit 5}
                                               :links {:vocab {:mine (clj-name (or @current-mine-name ""))}
                                                       :url (fn [vocab] (str "#/reportpage/"
                                                                             (:mine vocab) "/"
                                                                             (:class vocab) "/"
                                                                             (:objectId vocab)))}}}]))
                          @runnable-templates))
               (into [:div.container]
                     (map (fn [{:keys [name referencedType displayName] :as x}]
                            (let [key (str id name)]
                              ^{:key key} [tbl {:loc [:report-page id name]
                                                :service (:service @service)
                                                :title displayName
                                                :query {:from type
                                                        :select (map (partial str type "." name ".") (map strip-class (get @summary-fields (keyword referencedType))))
                                                        :where [{:path (str type ".id")
                                                                 :op "="
                                                                 :value id}]}
                                                :settings {:pagination {:limit 5}
                                                           :links {:vocab {:mine (clj-name (or @current-mine-name ""))}
                                                                   :url (fn [vocab] (str "#/reportpage/"
                                                                                         (:mine vocab) "/"
                                                                                         (:class vocab) "/"
                                                                                         (:objectId vocab)))}}}]))
                          (concat non-empty-references non-empty-collections)))])]))])))
