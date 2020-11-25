(ns bluegenes.pages.reportpage.components.sidebar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.components.bootstrap :refer [poppable]]
            [bluegenes.pages.reportpage.subs :as subs]
            [bluegenes.route :as route]))

(def ^:const entries-to-show 5)

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
