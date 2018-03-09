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


(defn tbl []
  (r/create-class
    {:component-did-mount (fn [this]
                            (let [{:keys [loc path constraint] :as data} (r/props this)]

                              (dispatch [:im-tables/load loc (dissoc data :loc)])))
     :reagent-render (fn [{:keys [loc]}]
                       [:div {:style {:background-color "white"}} [im-table/main loc]])}))

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
       (let [collections (vals (get-in @model [:classes (keyword (:type @params)) :collections]))]

         (let [{:keys [type id]} @params]
           (into [:div]
                 (map (fn [{:keys [name referencedType]}]
                        [tbl {:loc [:report-page id name]
                              :service (:service @service)
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
                                                                       (:objectId vocab)))}}}])
                      collections))))
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
