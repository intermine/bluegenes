(ns bluegenes.sections.home.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [bluegenes.sections.home.circles :as circles]))

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
     [circles/main-panel]
     ]))
