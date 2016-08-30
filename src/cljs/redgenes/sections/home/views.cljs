(ns redgenes.sections.home.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [redgenes.components.search :as search]
            [redgenes.components.templates.views :as templates]
            [redgenes.components.lists.views :as lists]
            [redgenes.components.icons :as icons]
            [redgenes.sections.home.circles :as circles]
            [redgenes.sections.home.texty :as texty]
            [redgenes.components.search :as search]))

(defn footer []
  (fn []
    [:footer.footer
     [:div
      [:p "Powered by: "
       [:a {:href "nope"}
        [:img {:width "120px" :src "https://cdn.rawgit.com/intermine/design-materials/master/logos/intermine/intermine.png"}]]]
      [:a {:href "nope"} "Cite"]
      [:a {:href "nope"} "Contact"]
      [:a {:href "nope"} "Blog"]]
     [:div [:p "Funded by:"]
      [:a {:href "nope"} "Wellcome Trust"]
      [:a {:href "nope"} "NIH"]
      ]]))

(defn main []
  (fn []
    [:div.approot
     [icons/icons]
     [circles/main-panel]
     ]))
