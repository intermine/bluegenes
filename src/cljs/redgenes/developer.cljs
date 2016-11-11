(ns redgenes.developer
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [redgenes.components.icons :as icons]
            [redgenes.sections.assets.views :as assets]
            [accountant.core :refer [navigate!]]))

(defn mine-config [mine-url mine-name]
  (let [current-mine (subscribe [:current-mine])]
    (fn []
      [:div.panel.container [:h3 "Current mine: "]
       [:p (:name (:mine (@mine-name @(subscribe [:mines])))) " at "
        [:a {:href @mine-url} @mine-url]]
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
        ;;this needs more work in the form of a default organism for queries like homologues and ID resolution. If you really need to add other organisms just go to the mines.cljc file and add it there thankyou please.
        ; [:div.form-group
        ;  [:label "Paste a new mine URL here if it's not in the list above: "
        ;    [:input.form-control
        ;      {:type      "Text"
        ;      :defaultValue     "http://"
        ;      :on-change (fn [e] (dispatch [:new-temporary-mine (.. e -target -value) (keyword "Other")]))}]]]
        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [e] (.preventDefault e))} "Save"]]
       ])))

(defn old-stuff []
  [:div
   (json-html/edn->hiccup @(subscribe [:mines]))
   #_[:div.title "Routes"]
   #_[:div.btn-toolbar
      [:button.btn {:on-click #(navigate! "#/assets/lists/123")} "Asset: List: (123)"]
      [:button.btn {:on-click #(navigate! "#/objects/type/12345")} "Object (12345)"]
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
  (let [mine-url  (re-frame/subscribe [:mine-url])
        mine-name (re-frame/subscribe [:mine-name])]
    (fn []
      [:div.developer
       [mine-config mine-url mine-name]
       [iconview]
       ])))
