(ns bluegenes.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [bluegenes.developer :as dev]
            [bluegenes.components.navbar.nav :as nav]
            [bluegenes.components.icons :as icons]
            [bluegenes.sections.home.views :as home]
            [bluegenes.components.search.views :as search]
            [bluegenes.effects]
            [bluegenes.sections.reportpage.views :as reportpage]
            [bluegenes.components.templates.views :as templates]
            [bluegenes.sections.querybuilder.views :as qb]
            [bluegenes.sections.mymine.views.main :as mymine]
            [bluegenes.components.ui.alerts :as alerts]
            [bluegenes.components.idresolver.views :as idresolver]
            [bluegenes.sections.results.views :as results]
            [bluegenes.sections.regions.views :as regions]
            [bluegenes.sections.help.views :as help]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [ocall oapply oget oset!]]
            ))

;; about
(enable-console-print!)

(defn footer []
  (fn []
    [:footer.footer
     [:div
      [:p "BlueGenes (alpha) powered by: "
       [:a {:href "http://www.intermine.org"}
        [:img {:width "120px" :src "https://cdn.rawgit.com/intermine/design-materials/c4716412/logos/intermine/intermine.png" :alt "InterMine"}]]]
      [:a {:href "https://intermineorg.wordpress.com/cite/"} "Cite"]
      [:a {:href "http://intermine.readthedocs.io/en/latest/about/contact-us/"} "Contact"]
       [:a {:href "http://chat.intermine.org/"} "Chat"]
      [:a {:href "https://intermineorg.wordpress.com/"} "Blog"]
       [:a {:on-click #(navigate! "/help")} [:svg.icon.icon-question [:use {:xlinkHref "#icon-question"}]] " Help"]]
     [:div [:p "Funded by:"]
      [:a {:href "http://www.wellcome.ac.uk/" :target "_blank"} "Wellcome Trust"]
      [:a {:href "https://www.nih.gov/" :target "_blank"} "NIH"]
      ]]))

;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home/main])
(defmethod panels :debug-panel [] [dev/debug-panel])
(defmethod panels :templates-panel [] [templates/main])
(defmethod panels :reportpage-panel [] [reportpage/main])
(defmethod panels :upload-panel [] [idresolver/main])
(defmethod panels :search-panel [] [search/main])
(defmethod panels :results-panel [] [results/main])
(defmethod panels :regions-panel [] [regions/main])
(defmethod panels :mymine-panel [] [mymine/main])
(defmethod panels :help-panel [] [help/main])
(defmethod panels :querybuilder-panel [] [qb/main])
(defmethod panels :default [] [home/main])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])
        ;;note: I think we can do better than this loader - perhaps a static html first page
        first-blush-loader (ocall js/document "getElementById"  "wrappy")]
    (fn []
      (cond first-blush-loader (ocall first-blush-loader "remove" ))
      [:div.approot
       [icons/icons]
       [nav/main]
       [:main [show-panel @active-panel]]
       [footer]
       [alerts/invalid-token-alert]
       [alerts/messages]])))
