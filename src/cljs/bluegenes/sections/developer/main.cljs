(ns bluegenes.developer.main
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.developer.icons :as icons]
            [bluegenes.persistence :as persistence]
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
    [:div.developer
     [mine-config]
     [localstorage-destroyer]
     [version-number]
     [icons/iconview]]))
