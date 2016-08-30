(ns redgenes.sections.home.views
  (:require [re-frame.core :as re-frame :refer [subscribe]]
            [redgenes.components.search :as search]
            [redgenes.components.templates.views :as templates]
            [redgenes.components.lists.views :as lists]
            [redgenes.components.icons :as icons]
            [redgenes.sections.home.circles :as circles]
            [redgenes.sections.home.texty :as texty]
            [redgenes.components.search :as search]))




(defn generic-section []
  (fn []
    [:div.panel
     [:h2 "Some Component"]
     [:ul.list-group
      [:li.list-group-item "Data"]
      [:li.list-group-item "More Data"]
      [:li.list-group-item "And some more data"]]]))

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

(defn header []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.header
       ;[search/main]
       ])))

(defn welcome []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div.welcome
       [:h3 "Welcome to Intermine"]])))

(defn main []
  (fn []
    [:div.approot.red
     [icons/icons]
     ;[:svg.icon [:use {:xlinkHref "#icon-floppy-disk"}]]
     ;[welcome]
     [header]
     [circles/main-panel]
     [:div.container.padme]
     [footer]
     ]))

