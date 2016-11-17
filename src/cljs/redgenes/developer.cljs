(ns redgenes.developer
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [redgenes.components.icons :as icons]
            [redgenes.persistence :as persistence]
            [accountant.core :refer [navigate!]]))

(defn mine-config []
  (let [current-mine (subscribe [:current-mine])
        mines (subscribe [:mines])
        url (str "http://" (:root (:service @current-mine)))]
    (fn []
      [:div.panel.container [:h3 "Current mine: "]
       [:p (:name @current-mine) " at "
        [:a {:href url} url]]
       [:form
        [:legend "Select a new mine to draw data from:"]
        (into [:div.form-group.mine-choice
               {:on-change (fn [e]
                             (dispatch [:set-active-mine (keyword (aget e "target" "value"))]))
                :value     "select-one"}]
              (map (fn [[id details]]
                     [:label
                      {:class (cond (= id (:id @current-mine)) "checked")}
                      [:input
                       {:type           "radio"
                        :name           "urlradios"
                        :id             id
                        :defaultChecked (= id (:id @current-mine))
                        :value          id}] (:common details)]) @(subscribe [:mines])))
        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [e] (.preventDefault e))} "Save"]]
       ])))

 (defn localstorage-destroyer []
     (fn []
       [:div.panel.container [:h3 "Delete local storage: "]
        [:form
         [:p "This will delete the local storage settings included preferred intermine instance, model, lists, and summaryfields. Model, lists, summaryfields should be loaded afresh every time anyway, but here's the easy pressable button to be REALLY SURE: "]
         [:button.btn.btn-primary.btn-raised
          {:on-click
           (fn [e]
             (.preventDefault e)
             (persistence/destroy!)
             (.reload js/document.location true)
             )} "Delete RedGenes localstorage... for now."]]
        ]))

(defn old-stuff []
  [:div
   (json-html/edn->hiccup @(subscribe [:mines]))
   #_[:div.title "Routes"]
   #_[:div.btn-toolbar
      [:button.btn {:on-click #(navigate! "#/assets/lists/123")} "Asset: List: (123)"]
      [:button.btn {:on-click #(navigate! "#/reportpage/type/12345")} "Object (12345)"]
      [:button.btn {:on-click #(navigate! "#/listanalysis/list/PL FlyAtlas_midgut_top")} "List (PL FlyAtlas_midgut_top)"]]
   #_[:div.panel.container
      [:div.title "Global Progress Bar"]
      [:button.btn
       {:on-click #(dispatch [:test-progress-bar (rand-int 101)])} "Random"]
      [:button.btn
       {:on-click #(dispatch [:test-progress-bar 0])} "Hide"]]
   ])

(defn iconview []
  [:div.panel.container [:h3 "All icons defs in the icons file (components/icons.cljs.)"]
   (let [icon-names (rest (last (icons/icons)))]
     [:table.icon-view [:tbody
                        (map (fn [[icon-symbol]]
                               (let [icon-name (last (clojure.string/split icon-symbol "#"))]
                                 [:tr {:key icon-name}
                                  [:td [:svg.icon {:class icon-name} [:use {:xlinkHref (str "#" icon-name)}]]]
                                  [:td icon-name]
                                  [:td [:div.code "[:svg.icon." icon-name " [:use {:xlinkHref \"#" icon-name "\"}]]"]]]
                                 )) icon-names)
                        ]])])

(defn debug-panel []
    (fn []
      [:div.developer
       [mine-config]
       [localstorage-destroyer]
       [iconview]
       ]))
