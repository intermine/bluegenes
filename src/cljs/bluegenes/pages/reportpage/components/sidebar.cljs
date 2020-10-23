(ns bluegenes.pages.reportpage.components.sidebar
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [bluegenes.components.icons :refer [icon-comp]]
            [bluegenes.components.bootstrap :refer [poppable]]))

(defn main []
  [:div.sidebar
   [:div.sidebar-entry
    [:h4 "Lists"]
    [:ul
     [:li [:a "PL FlyTF_PWM_TFs (129)"]]
     [:li [:a "PL FlyTF_putativeTFs (757)"]]]]
   [:div.sidebar-entry
    [:h4 "Other mines"]
    [:ul
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
                                [:div.icon-background
                                 [icon-comp "external"]]]}]]]]
   [:div.sidebar-entry
    [:h4 "External resources"]
    [:ul
     [:li [:a "Ensembl"]]
     [:li [:a "BioGRID"]]]]])
