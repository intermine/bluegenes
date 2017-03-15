(ns redgenes.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [redgenes.developer :as dev]
            [redgenes.components.navbar.nav :as nav]
            [redgenes.components.icons :as icons]
            [redgenes.sections.home.views :as home]
            [redgenes.components.search.views :as search]
            [redgenes.effects]
            [redgenes.sections.reportpage.views :as reportpage]
            [redgenes.components.templates.views :as templates]
            [redgenes.components.querybuilder.views.main :as querybuilder]
            [redgenes.sections.querybuilder.views :as qb]
            [redgenes.components.toast :as toast]
            [redgenes.components.ui.alerts :as alerts]
            [redgenes.components.idresolver.views :as idresolver]
            [redgenes.components.databrowser.views :as explore]
            [redgenes.sections.results.views :as results]
            [redgenes.sections.lists.views :as lists]
            [redgenes.sections.regions.views :as regions]
            [redgenes.sections.saveddata.views :as saved-data]
            [redgenes.sections.help.views :as help]
            [accountant.core :refer [navigate!]]
            [oops.core :refer [ocall oapply oget oset!]]
            ))

;; about
(enable-console-print!)
(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a.callout {:on-click #(navigate! "/")} "go to Home Page"]]]))


(defn footer []
  (fn []
    [:footer.footer
     [:div
      [:p "Powered by: "
       [:a {:href "nope"}
        [:img {:width "120px" :src "https://cdn.rawgit.com/intermine/design-materials/master/logos/intermine/intermine.png"}]]]
      [:a {:href "https://intermineorg.wordpress.com/cite/"} "Cite"]
      [:a {:href "http://intermine.readthedocs.io/en/latest/about/contact-us/"} "Contact"]
      [:a {:href "https://intermineorg.wordpress.com/"} "Blog"]]
     [:div [:p "Funded by:"]
      [:a {:href "http://www.wellcome.ac.uk/" :target "_blank"} "Wellcome Trust"]
      [:a {:href "https://www.nih.gov/" :target "_blank"} "NIH"]
      ]]))

;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home/main])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :debug-panel [] [dev/debug-panel])
(defmethod panels :templates-panel [] [templates/main])
(defmethod panels :reportpage-panel [] [reportpage/main])
(defmethod panels :upload-panel [] [idresolver/main])
(defmethod panels :search-panel [] [search/main])
(defmethod panels :results-panel [] [results/main])
(defmethod panels :regions-panel [] [regions/main])
(defmethod panels :saved-data-panel [] [lists/main])
(defmethod panels :explore-panel [] [explore/main])
(defmethod panels :help-panel [] [help/main])
(defmethod panels :querybuilder-panel [] [qb/main])
;(defmethod panels :querybuilder-panel [] [:div.container [querybuilder/main]])
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
       [toast/main]
       [alerts/invalid-token-alert]


       ])))
