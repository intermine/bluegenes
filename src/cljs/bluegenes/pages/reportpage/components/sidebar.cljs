(ns bluegenes.pages.reportpage.components.sidebar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.pages.reportpage.subs :as subs]
            [bluegenes.pages.reportpage.events :as events]
            [bluegenes.route :as route]
            [bluegenes.components.loader :refer [mini-loader]]
            [bluegenes.components.navbar.nav :refer [logo-path]]
            [bluegenes.utils :refer [compatible-version?]]
            [oops.core :refer [ocall]]
            [clojure.string :as str]))

(def ^:const entries-to-show 5)

(defn generate-permanent-url [& {:keys [collapsed? type]}]
  (let [{:keys [status url error]} @(subscribe [::subs/share])
        api-version @(subscribe [:api-version])
        input-ref* (atom nil)]
    [:li [:span.dropdown
          [:a.sidebar-action.dropdown-toggle
           {:data-toggle "dropdown"
            :role "button"
            :on-click #(dispatch [::events/generate-permanent-url api-version type])}
           (case type
             :rdf [:<> [icon-comp "file"] "Copy URL to RDF document"]
             [:<> [icon-comp "price-tag"] "Copy permanent URL"])]
          [:div.dropdown-menu
           {:class (when-not collapsed? :dropdown-menu-sidebar)}
           (if status
             [:form {:on-submit #(.preventDefault %)}
              (case status
                :success [:div.permanent-url-container
                          [:p "Save this permanent URL to keep a reference to this report.  The URL will continue to work even when new versions of the database are released."
                           [poppable {:data "Permanent URLs use identifiers unique to the object, and need to be resolved to an object ID by InterMine. This makes them less suitable than object IDs for daily use, but allows them to stay valid in new versions of the database."
                                      :children [icon-comp "info"]}]]
                          [:input.form-control
                           {:type "text"
                            :ref (fn [el]
                                   (when el
                                     (reset! input-ref* el)
                                     (.focus el)
                                     (.select el)))
                            :autoFocus true
                            :readOnly true
                            :style {:width (* (count url) 8)}
                            :value url}]
                          [:button.btn.btn-raised
                           {:on-click (fn [_]
                                        (when-let [input-el @input-ref*]
                                          (.focus input-el)
                                          (.select input-el)
                                          (try
                                            (ocall js/document :execCommand "copy")
                                            (catch js/Error _))))}
                           "Copy"]]
                :failure [:div.permanent-url-container
                          [:p.failure "Failed to generate permanent URL."
                           (when-let [err (not-empty error)]
                             [:code err])]])]
             [mini-loader])]]]))

(defn actions [collapsed?]
  [:ul.sidebar-actions
   [generate-permanent-url :collapsed? collapsed?]
   #_[generate-permanent-url :collapsed? collapsed? :type :rdf]])

(defn entry []
  (let [show-all* (reagent/atom false)]
    (fn [{:keys [title error]} children]
      [:div.sidebar-entry.col-sm-6.col-lg-12
       [:h4 title]
       (when error
         [:p error])
       [:ul
        (cond->> children
          (not @show-all*) (take entries-to-show))]
       (when (> (count children) entries-to-show)
         [:div.show-more
          [:button.btn.btn-link
           {:on-click #(swap! show-all* not)}
           (if @show-all*
             "Show less"
             (str "Show " (- (count children) entries-to-show) " more"))]])])))

(defn lists-containing []
  (let [lists @(subscribe [::subs/report-lists])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])]
    [entry (merge
            {:title "Lists"}
            (when (empty? lists)
              {:error (str "This " rootClass " isn't in any lists.")}))
     (doall
      (for [{:keys [title size id description]} lists]
        ^{:key id}
        [:li [poppable {:data description
                        :children [:a {:href (route/href ::route/results {:title title})}
                                   (str title " (" size ")")
                                   [:div.fade-background]]}]]))]))

(defn other-mines []
  (let [expanded-mines* (reagent/atom #{})]
    (fn []
      (let [mines @(subscribe [::subs/report-homologues])
            {:keys [type]} @(subscribe [:panel-params])]
        (when (= type "Gene")
          [entry (merge
                  {:title "Other mines"}
                  (when (empty? mines)
                    {:error "This mine does not have any neighbours."}))
           (doall
            (for [[mine-kw {:keys [mine loading? homologues error]}] (sort-by (comp name key) mines)
                  :let [expanded? (@expanded-mines* mine-kw)]]
              ^{:key (name mine-kw)}
              [:li.other-mine
               (-> [:div [:strong.mine (:name mine)]]
                   (into (cond
                           loading?
                           [[mini-loader "tiny"]]

                           error
                           [[poppable {:data error
                                       :children [:span "Error"]}]]

                           (seq homologues)
                           (for [[organism genes] (cond->> homologues
                                                    (not expanded?) (take entries-to-show))]
                             (into [:div
                                    [:span.organism organism]]
                                   (for [gene genes
                                         :let [nom (some gene [:symbol :primaryIdentifier :secondaryIdentifier])]]
                                     (if (:external? mine)
                                       [:a {:target "_blank"
                                            :href (str (:url mine) "/report.do?id=" (:id gene))}
                                        nom
                                        [icon-comp "external"]]
                                       [:a {:href (route/href ::route/report {:mine mine-kw
                                                                              :type "Gene"
                                                                              :id (:id gene)})}
                                        nom]))))

                           :else
                           [[:span "No results"]]))
                   (cond-> (and (> (count homologues) entries-to-show) (not expanded?))
                     (conj [:a.show-all-homologues
                            {:on-click #(swap! expanded-mines* conj mine-kw)
                             :role "button"}
                            (let [remaining (- (count homologues) entries-to-show)]
                              (str "Show " remaining " more organism" (when (> remaining 1) "s")))])))

               [:img {:src (or (get-in mine [:images :logo]) ; Only available for registry mines.
                               (str (:url mine) logo-path))}]]))])))))

(defn data-sources []
  (let [sources @(subscribe [::subs/report-sources])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])]
    [entry (merge
            {:title [:span "Data sources"
                     [:a {:on-click #(dispatch [:home/query-data-sources])
                          :role "button"}
                      "[View all]"]]}
            (when (empty? sources)
              {:error (str "No data sources available for this " rootClass ".")}))
     (doall
      (for [{description :dataSets.description url :dataSets.url
             name :dataSets.name id :dataSets.id} sources]
        ^{:key id}
        [:li [poppable {:data description
                        :children [:a (if url
                                        {:href url :target "_blank"}
                                        {:class :disabled})
                                   name
                                   [:div.fade-background
                                    (when url
                                      [icon-comp "external"])]]}]]))]))

(defn external-resources []
  (let [links @(subscribe [::subs/report-external-links])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])
        im-version @(subscribe [:current-intermine-version])]
    [entry (merge
            {:title "External resources"}
            (cond
              (not (compatible-version? "5.0.0" im-version))
              {:error "This mine is running an older InterMine version which does not support external resources in BlueGenes."}

              (empty? links)
              {:error (str "No external resources available for this " rootClass ".")}))
     (doall
      (for [{:keys [linkId title url]} links]
        ^{:key linkId}
        [:li
         [:a {:href url :target "_blank"}
          title
          [:div.fade-background
           [icon-comp "external"]]]]))]))

(defn main []
  [:div.sidebar
   [:div.row
    [:div.sidebar-entry.col-sm-12.visible-lg-block
     [actions false]]
    [:div.sidebar-entry.col-sm-12.visible-sm-block.visible-md-block
     [actions true]]
    [lists-containing]
    [other-mines]
    [:div.clearfix.visible-sm-block.visible-md-block]
    [data-sources]
    [external-resources]]])
