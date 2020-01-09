(ns bluegenes.pages.developer.devhome
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.pages.developer.events :as events]
            [bluegenes.pages.developer.subs :as subs]
            [bluegenes.pages.developer.icons :as icons]
            [bluegenes.pages.developer.tools :as tools]
            [clojure.string :refer [blank?]]
            [bluegenes.route :as route]
            [cljs-bean.core :refer [->clj]]))

(defn nav []
  "Buttons to choose which mine you're using."
  [:ul.dev-navigation
   [:li [:a {:href (route/href ::route/debug {:panel "main"})}
         [:svg.icon.icon-cog
          [:use {:xlinkHref "#icon-cog"}]] "Debug Console"]]
   [:li
    [:a {:href (route/href ::route/debug {:panel "tool-store"})}
     [:svg.icon.icon-star-full
      [:use {:xlinkHref "#icon-star-full"}]] "Tool 'App Store'"]]
   [:li [:a {:href (route/href ::route/debug {:panel "icons"})}
         [:svg.icon.icon-intermine
          [:use {:xlinkHref "#icon-intermine"}]] "Icons"]]])

(defn mine-config []
  "Outputs current intermine and list of mines from registry
   To allow users to choose their preferred InterMine."
  (let [current-mine (subscribe [:current-mine])]
    (fn []
      [:div.panel.container [:h3 "Current mine: "]
       [:p (:name @current-mine) " at "
        [:span (:root (:service @current-mine))]]
       [:form
        [:legend "Select a new mine to draw data from:"]
        (into
         [:div.form-group.mine-choice
          [:label
           {:class "checked"}
           [:input
            {:type           "radio"
             :name           "urlradios"
             :id             (:id @current-mine)
             :defaultChecked true
             :value          (:id @current-mine)}]
           (:name @current-mine) " (current)"]]
         (map
          (fn [[id details]]
            (cond
              (not= id (:id @current-mine))
              (let [mine-name
                    (if (blank? (:name details))
                      id (:name details))]
                [:label {:title (:description details)}
                 [:input
                  {:on-change
                   (fn [e]
                     (dispatch
                      [::route/navigate
                       ::route/home
                       {:mine (-> details :namespace keyword)}]))
                   :type           "radio"
                   :name           "urlradios"
                   :id             id
                   :value          id}] mine-name])))
          @(subscribe [:registry])))
        [:button.btn.btn-primary.btn-raised
         {:on-click (fn [e] (.preventDefault e))} "Save"]]])))

(defn version-number []
  [:div.panel.container
   [:h3 "Client Version: "]
   [:code (:version (->clj js/serverVars))]])

(defn localstorage-destroyer []
  (fn []
    [:div.panel.container [:h3 "Delete local storage: "]
     [:form
      [:p "This will delete the local storage settings included preferred intermine instance, model, lists, and summaryfields. Model, lists, summaryfields should be loaded afresh every time anyway, but here's the easy pressable button to be REALLY SURE: "]
      [:button.btn.btn-primary.btn-raised
       {:on-click
        (fn [e]
          (.preventDefault e)
          ;; The usual way you'd destroy state is with an event handler
          ;; specifically for this, but since this is a "debugging" feature,
          ;; and we want to reload the page after, will just do it directly.
          (.removeItem js/localStorage ":bluegenes/state")
          (.reload js/document.location true))}
       "Delete bluegenes localstorage... for now."]]]))

(defn scrambled-eggs-and-token []
  (let [token (subscribe [:active-token])]
    (fn []
      [:div.panel.container
       [:h3 "Token"]
       [:p "The current token for your current InterMine is:"]
       [:pre @token]
       [:p "Don't press the scramble token button unless you have been told to, or you're developing token-related code!"]
       [:button.btn.btn-primary.btn-raised
        {:type "button"
         :on-click
         (fn [e]
           (.preventDefault e)
           (.log js/console "%cscrambling dat token")
           (dispatch [:scramble-tokens]))}
        "Scramble token"]])))

(defn tool-api-path []
  (let [tools-path @(subscribe [::subs/tools-path])]
    [:div.panel.container
     [:h3 "Tools path"]
     [:p "The path where your BlueGenes tools are installed on the server is:"]
     [:pre tools-path]]))

(defn debug-panel []
  (fn []
    (let [panel (subscribe [::subs/panel])]
      [:div.developer.container
       [nav]
       (case @panel
         "main" [:div
                 [:h1 "Debug console"]
                 [mine-config]
                 [localstorage-destroyer]
                 [scrambled-eggs-and-token]
                 [tool-api-path]
                 [version-number]]
         "tool-store" [tools/tool-store]
         "icons" [icons/iconview])])))
