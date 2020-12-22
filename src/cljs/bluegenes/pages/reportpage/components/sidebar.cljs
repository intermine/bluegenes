(ns bluegenes.pages.reportpage.components.sidebar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.pages.reportpage.subs :as subs]
            [bluegenes.pages.reportpage.events :as events]
            [bluegenes.route :as route]
            [bluegenes.components.loader :refer [mini-loader]]
            [oops.core :refer [ocall]]))

(def ^:const entries-to-show 5)

(defn generate-permanent-url [collapsed?]
  (let [{:keys [status url error]} @(subscribe [::subs/share])
        input-ref* (atom nil)]
    [:li [:span.dropdown
          [:a.sidebar-action.dropdown-toggle
           {:data-toggle "dropdown"
            :role "button"
            :on-click #(dispatch [::events/generate-permanent-url])}
           [icon-comp "price-tag"] "Copy permanent URL"]
          [:div.dropdown-menu
           {:class (when-not collapsed? :dropdown-menu-right)}
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
   [generate-permanent-url collapsed?]])

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

(defn data-sources []
  (let [sources @(subscribe [::subs/report-sources])
        {:keys [rootClass]} @(subscribe [::subs/report-summary])]
    [entry (merge
            {:title "Data sources"}
            (when (empty? sources)
              {:error (str "No data sources available for this " rootClass ".")}))
     (doall
      (for [{:keys [description url name id]} sources]
        ^{:key id}
        [:li [poppable {:data description
                        :children [:a {:href url
                                       :target "_blank"}
                                   name
                                   [:div.fade-background
                                    [icon-comp "external"]]]}]]))]))

(defn main []
  [:div.sidebar
   [:div.row
    [:div.sidebar-entry.col-sm-12.visible-lg-block
     [actions false]]
    [:div.sidebar-entry.col-sm-12.visible-sm-block.visible-md-block
     [actions true]]
    [lists-containing]
    [:div.sidebar-entry.col-sm-6.col-lg-12
     [:h4 "Other mines"]
     [:ul
      [:li [:a "Dev note: Work In Progress!!!"]]
      [:li [:a "Humanmine"]]
      [:li [:a "Flymine"]]]]
    [:div.clearfix.visible-sm-block.visible-md-block]
    [data-sources]
    [:div.sidebar-entry.col-sm-6.col-lg-12
     [:h4 "External resources"]
     [:ul
      [:li [:a "Dev note: Work In Progress!!!"
            [:div.fade-background
             [icon-comp "external"]]]]
      [:li [:a "Ensembl"
            [:div.fade-background
             [icon-comp "external"]]]]
      [:li [:a "BioGRID"
            [:div.fade-background
             [icon-comp "external"]]]]]]]])
