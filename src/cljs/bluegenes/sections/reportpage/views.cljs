(ns bluegenes.sections.reportpage.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [reagent.core :as r]
            [bluegenes.sections.reportpage.components.summary :as summary]
            [bluegenes.components.table :as table]
            [bluegenes.components.lighttable :as lighttable]
            [bluegenes.components.loader :refer [loader]]
            [bluegenes.sections.reportpage.components.minelinks :as minelinks]
            [accountant.core :refer [navigate!]]
            [bluegenes.sections.reportpage.events :as evts]
            [bluegenes.sections.reportpage.subs :as subs]
            [im-tables.views.core :as im-table]
            [imcljs.path :as im-path]))





#_(defn main-panel []
    ; Increase these range to produce N number of tables on the same page
    ; (useful for stress testing)
    (let [number-of-tables 2
          reboot-tables-fn (fn [] (dotimes [n number-of-tables]
                                    (dispatch [:im-tables/load [:test :location n] (build-configuation)])))]
      (r/create-class
        {:component-did-mount reboot-tables-fn
         :reagent-render (let [show? (r/atom true)]
                           (fn []
                             (into [:div]
                                   (->> (range 0 number-of-tables)
                                        (map (fn [n] [im-table/main [:test :location n]]))))))})))


(defn tbl [{:keys [loc]}]
  (let [data (subscribe [::subs/a-table loc])]
    (r/create-class
      {:component-did-mount (fn [this]
                              (let [{:keys [loc path constraint] :as data} (r/props this)]

                                (dispatch [:im-tables/load loc (dissoc data :loc)])))
       :reagent-render (fn [{:keys [loc title all]}]
                         (let [result-count (get-in @data [:response :iTotalRecords])]
                           [:div
                            [:h3 (str title (when result-count (str " (" result-count ")")))]
                            (if (= 0 result-count)
                              [:div "No Results"]
                              [:div {:style {:background-color "white"}}
                               [im-table/main loc]])]))})))

(defn strip-class [s]
  (clojure.string/join "." (drop 1 (clojure.string/split s "."))))

(defn main []
  (let [params (subscribe [:panel-params])
        report (subscribe [:report])
        categories (subscribe [:template-chooser-categories])
        templates (subscribe [:runnable-templates])
        service (subscribe [:current-mine])
        ;collections (subscribe [:collections])
        model (subscribe [:current-model])
        fetching-report? (subscribe [:fetching-report?])
        summary-fields (subscribe [:current-summary-fields])]
    (fn []
      [:div.container.report
       (let [collections (vals (get-in @model [:classes (keyword (:type @params)) :collections]))
             references (vals (get-in @model [:classes (keyword (:type @params)) :references]))
             non-empty-collections (filter (fn [c]
                                             (> (get-in @model [:classes (keyword (:referencedType c)) :count]) 0)) collections)
             non-empty-references (filter (fn [c]
                                            (> (get-in @model [:classes (keyword (:referencedType c)) :count]) 0)) references)]

         (let [{:keys [type id]} @params]
           [:div
            [:h1 "References"]
            (into [:div.container]
                  (map (fn [{:keys [name referencedType displayName] :as x}]
                         (let [key (str id name)]
                           ^{:key key} [tbl {:loc [:report-page id name]
                                             :service (:service @service)
                                             :title displayName
                                             :all x
                                             :query {:from type
                                                     :select (map (partial str type "." name ".") (map strip-class (get @summary-fields (keyword referencedType))))
                                                     :where [{:path (str type ".id")
                                                              :op "="
                                                              :value id}]}
                                             :settings {:pagination {:limit 10}
                                                        :links {:vocab {:mine "BananaMine"}
                                                                :url (fn [vocab] (str "#/reportpage/"
                                                                                      (:mine vocab) "/"
                                                                                      (:class vocab) "/"
                                                                                      (:objectId vocab)))}}}]))
                       non-empty-references))
            [:h1 "Collections"]
            (into [:div.container]
                  (map (fn [{:keys [name referencedType displayName] :as x}]
                         (let [key (str id name)]
                           ^{:key key} [tbl {:loc [:report-page id name]
                                             :service (:service @service)
                                             :title displayName
                                             :all x
                                             :query {:from type
                                                     :select (map (partial str type "." name ".") (map strip-class (get @summary-fields (keyword referencedType))))
                                                     :where [{:path (str type ".id")
                                                              :op "="
                                                              :value id}]}
                                             :settings {:pagination {:limit 10}
                                                        :links {:vocab {:mine "BananaMine"}
                                                                :url (fn [vocab] (str "#/reportpage/"
                                                                                      (:mine vocab) "/"
                                                                                      (:class vocab) "/"
                                                                                      (:objectId vocab)))}}}]))
                       non-empty-collections))]
           ))
       #_(if @fetching-report?
           [loader (str (:type @params) " Report")]
           (into [:div]
                 (map (fn [c] [tbl {:loc c
                                    :attribute c
                                    :id (:id @params)
                                    :type (:type @params)
                                    :summary-fields @summary-fields}])
                      @collections))

           #_[:div
              [:ol.breadcrumb
               [:li [:a {:href "#/" :on-click #(navigate! "/")} "Home"]]
               [:li [:a {:href "#/search" :on-click #(navigate! "/search")} "Search Results"]]
               [:li.active [:a "Report"]]]
              [:button.btn.btn-primary.btn-raised
               {:on-click (fn [])}
               "Run"]
              [summary/main (:summary @report)]
              (cond (= "Gene" (:type @params))
                    [minelinks/main (:id @params)])
              (into [:div.collections] (map (fn [query] [lighttable/main query {:title true}]) @collections))
              (into [:div.templates] (map (fn [[id details]] [table/main details]) @templates))])])))
