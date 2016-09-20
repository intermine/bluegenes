(ns redgenes.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [json-html.core :as json-html]
            [redgenes.components.nav :as nav]
            [redgenes.components.icons :as icons]
            [redgenes.sections.home.views :as home]
            [redgenes.sections.assets.views :as assets]
            [redgenes.sections.search.views :as search]
            [redgenes.sections.objects.views :as objects]
            [redgenes.sections.templates.views :as templates]
            [redgenes.components.querybuilder.views.main :as querybuilder]
            [redgenes.sections.upload.views :as upload]
            [redgenes.sections.explore.views :as explore]
            [redgenes.sections.analyse.views :as analyse]
            [redgenes.sections.results.views :as results]
            [redgenes.sections.saveddata.views :as saved-data]
            [redgenes.sections.help.views :as help]
            [accountant.core :refer [navigate!]]))

(defn debug-panel []
  (let [mine-url (re-frame/subscribe [:mine-url])]
    (fn []
      [:div
       [:div.panel.container
        [:input.form-control.input-lg
         {:type      "Text"
          :value     @mine-url
          :on-change (fn [e] (dispatch [:update-mine-url (.. e -target -value)]))}]
        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [] (re-frame/dispatch [:fetch-all-assets]))}
         "Update Assets"]
        #_[:div.title "Routes"]
        #_[:div.btn-toolbar
           [:button.btn {:on-click #(navigate! "#/assets/lists/123")} "Asset: List: (123)"]
           [:button.btn {:on-click #(navigate! "#/objects/type/12345")} "Object (12345)"]
           [:button.btn {:on-click #(navigate! "#/listanalysis/list/PL FlyAtlas_midgut_top")} "List (PL FlyAtlas_midgut_top)"]]]
       #_[:div.panel.container
          [:div.title "Global Progress Bar"]
          [:button.btn
           {:on-click #(dispatch [:test-progress-bar (rand-int 101)])} "Random"]
          [:button.btn
           {:on-click #(dispatch [:test-progress-bar 0])} "Hide"]]
       #_(json-html/edn->hiccup (dissoc @app-db :assets))])))


;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a.callout {:on-click #(navigate! "#/")} "go to Home Page"]]]))


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
(defmethod panels :debug-panel [] [debug-panel])
(defmethod panels :list-panel [] [assets/main])
(defmethod panels :templates-panel [] [templates/main])
(defmethod panels :object-panel [] [objects/main])
(defmethod panels :upload-panel [] [upload/main])
(defmethod panels :search-panel [] [search/main])
(defmethod panels :results-panel [] [results/main])
(defmethod panels :explore-panel [] [explore/main])
(defmethod panels :list-analysis-panel [] [analyse/main])
(defmethod panels :saved-data-panel [] [saved-data/main])
(defmethod panels :help-panel [] [help/main])
(defmethod panels :querybuilder-panel [] [:div.container [querybuilder/main]])
(defmethod panels :default [] [:div])

(defn show-panel
  [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.approot
       [icons/icons]
       [nav/main]
       [:main [show-panel @active-panel]]
       [footer]
       ])))
