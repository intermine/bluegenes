(ns redgenes.views
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
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
  (let [mine-url (re-frame/subscribe [:mine-url])
        mine-name (re-frame/subscribe [:mine-name])]
    (fn []
      [:div
        [:div.panel.container
          [:h3 "Current mine: "]
          [:p (:name (:mine (@mine-name @(subscribe [:mines])))) " at "
            [:a {:href @mine-url} @mine-url]]
          [:form


        [:legend "Select a new mine to draw data from:"]
          (into [:div.form-group.mine-choice
            {:on-change (fn [e] (dispatch [:set-active-mine (keyword (aget e "target" "value")) ]))
             :value "select-one"}
                 ]
            (map (fn [[id details]]
              [:label {:class (cond (= @mine-url (str "http://" (:url (:mine details)))) "checked")} [:input
               {:type "radio"
                :name "urlradios"
                :id id
                :defaultChecked (= @mine-url (str "http://" (:url (:mine details))))
                :value id} ] (:common details)]) @(subscribe [:mines])))
                ;;this needs more work in the form of a default organism for queries like homologues and ID resolution. 
                ; [:div.form-group
                ;  [:label "Paste a new mine URL here if it's not in the list above: "
                ;    [:input.form-control
                ;      {:type      "Text"
                ;      :defaultValue     "http://"
                ;      :on-change (fn [e] (dispatch [:new-temporary-mine (.. e -target -value) (keyword "Other")]))}]]]

        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [e]
                      (.preventDefault js/e)
                      )}
         "Save"]]
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
       #_(json-html/edn->hiccup @(subscribe [:mines]))])))


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
