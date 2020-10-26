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
      [:div.sidebar-entry
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

(defn main []
  [:div.sidebar
   [lists-containing]
   [:div.sidebar-entry
    [:h4 "Other mines"]
    [:ul
     [:li [:a "Dev note: Work In Progress!!!"]]
     [:li [:a "Humanmine"]]
     [:li [:a "Flymine"]]]]
   [:div.sidebar-entry
    [:h4 "Data sources"]
    [:ul
     [:li [poppable {:data "Gene family assignments for glyma.Wm82.gnm2 genes and proteins."
                     :children [:a
                                {:href "https://legumeinfo.org/data/public/Glycine_max/Wm82.gnm2.ann1.RVB6/"
                                 :target "_blank"}
                                "glyma.Wm82.gnm2.ann1.RVB6.legfed_v1_0.M65K.gfa.tsv"
                                [:div.fade-background
                                 [icon-comp "external"]]]}]]]]
   [:div.sidebar-entry
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
            [icon-comp "external"]]]]]]])
