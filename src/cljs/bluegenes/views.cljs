(ns bluegenes.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [bluegenes.pages.developer.devhome :as dev]
            [bluegenes.components.navbar.nav :as nav]
            [bluegenes.components.icons :as icons]
            [bluegenes.pages.home.views :as home]
            [bluegenes.components.search.views :as search]
            [bluegenes.effects]
            [bluegenes.pages.reportpage.views :as reportpage]
            [bluegenes.pages.templates.views :as templates]
            [bluegenes.pages.querybuilder.views :as qb]
            [bluegenes.pages.mymine.views.main :as mymine]
            [bluegenes.components.ui.alerts :as alerts]
            [bluegenes.components.idresolver.views :as idresolver]
            [bluegenes.pages.results.views :as results]
            [bluegenes.pages.regions.views :as regions]
            [bluegenes.pages.help.views :as help]
            [bluegenes.pages.profile.views :as profile]
            [bluegenes.components.loader :as loader]
            [bluegenes.route :as route]))

;; about


(enable-console-print!)

(defn footer []
  (fn []
    [:footer.footer
     [:div
      [:p "BlueGenes (alpha) powered by: "
       [:a {:href "http://www.intermine.org"}
        [:img {:width "120px" :src "https://raw.githubusercontent.com/intermine/design-materials/c4716412/logos/intermine/intermine.png" :alt "InterMine"}]]]
      [:a {:href "https://intermineorg.wordpress.com/cite/"} "Cite"]
      [:a {:href "http://intermine.readthedocs.io/en/latest/about/contact-us/"} "Contact"]
      [:a {:href "http://chat.intermine.org/" :target "_blank"} "Chat"]
      [:a {:href "https://intermineorg.wordpress.com/"} "Blog"]
      [:a {:href "https://github.com/intermine/" :target "_blank"} "GitHub"]
      [:a {:href (route/href ::route/help)}
       [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]] " Help"]]
     [:div [:p "Funded by:"]
      [:a {:href "http://www.wellcome.ac.uk/" :target "_blank"} "Wellcome Trust"]
      [:a {:href "https://www.nih.gov/" :target "_blank"} "NIH"]]]))

;; main

(defn show-panel [panel-name]
  [(case panel-name
     :home-panel         home/main
     :profile-panel      profile/main
     :debug-panel        dev/debug-panel
     :templates-panel    templates/main
     :reportpage-panel   reportpage/main
     :upload-panel       idresolver/main
     :search-panel       search/main
     :results-panel      results/main
     :regions-panel      regions/main
     :mymine-panel       mymine/main
     :help-panel         help/main
     :querybuilder-panel qb/main
     home/main)])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.approot
       [loader/mine-loader]
       [icons/icons]
       [nav/main]
       [:main [show-panel @active-panel]]
       [footer]
       [alerts/main]])))
