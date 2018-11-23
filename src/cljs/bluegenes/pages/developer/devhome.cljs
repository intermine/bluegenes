(ns bluegenes.pages.developer.devhome
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.pages.developer.events :as events]
            [bluegenes.pages.developer.subs :as subs]
            [bluegenes.pages.developer.icons :as icons]
            [bluegenes.pages.developer.tools :as tools]
            [bluegenes.persistence :as persistence]
            [clojure.string :refer [blank?]]
            [imcljs.internal.utils :as utils :refer [missing-http?-]]
            [accountant.core :refer [navigate!]]))

(defn nav []
  [:ul.dev-navigation
   [:li [:a {:on-click #(navigate! "/debug/main")}
         [:svg.icon.icon-cog [:use {:xlinkHref "#icon-cog"}]] "Debug Console"]]
   [:li
    [:a {:on-click #(navigate! "/debug/tool-store")}
     [:svg.icon.icon-star-full [:use {:xlinkHref "#icon-star-full"}]] "Tool 'App Store'"]]
   [:li [:a {:on-click #(navigate! "/debug/icons")}
         [:svg.icon.icon-intermine [:use {:xlinkHref "#icon-intermine"}]] "Icons"]]])

(defn mine-config []
  (let [current-mine (subscribe [:current-mine])
        mines (subscribe [:mines])
        minelink (:root (:service @current-mine))
        url (if missing-http?- (str "http://" minelink) minelink)]
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
                     (let [mine-name (if (blank? (:name details)) id (:name details))]
                     [:label
                      {:class (cond (= id (:id @current-mine)) "checked")}
                      [:input
                       {:type           "radio"
                        :name           "urlradios"
                        :id             id
                        :defaultChecked (= id (:id @current-mine))
                        :value          id}] mine-name])) @(subscribe [:mines]))
)        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [e] (.preventDefault e))} "Save"]]])))

(defn version-number []
  [:div.panel.container
   [:h3 "Client Version: "]
   [:code (str bluegenes.core/version)]])

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
          (.reload js/document.location true))} "Delete bluegenes localstorage... for now."]]]))

(defn debug-panel []
  (fn []
    (let [panel (subscribe [::subs/panel])]
      [:div.developer
       [nav]
       (cond
         (= @panel "main")
         [:div
          [:h1 "Debug console"]
          [mine-config]
          [localstorage-destroyer]
          [version-number]]
         (= @panel "tool-store")
         [tools/tool-store]
         (= @panel "icons")
         [icons/iconview])])))
