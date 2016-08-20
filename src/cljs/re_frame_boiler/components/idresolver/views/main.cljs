(ns re-frame-boiler.components.idresolver.views.main
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [json-html.core :as json-html]
            [dommy.core :as dommy :refer-macros [sel sel1]]
            [re-frame-boiler.components.idresolver.events]
            [re-frame-boiler.components.idresolver.subs]))

(def separators-map {:return 13
                     :space  32
                     :comma  188})

(def separators (vals separators-map))

(defn controls []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-primary
      {:on-click (fn [] (dispatch [:idresolver/resolve "mad"]))}
      "Upload"]]))

(defn input-box []
  (let [val (reagent/atom nil)]
    (fn []
      [:input.freeform
       {:type        "text"
        :placeholder "Identifiers..."
        :value       @val
        :on-change   (fn [e] (reset! val (.. e -target -value)))
        :on-key-down (fn [e] (let [k (.-keyCode e)]
                               (println k)
                               (if (some true? (map #(= k %) separators))
                                 (do
                                   (println "reseet")
                                   (dispatch [:idresolver/resolve @val])
                                   (reset! val "")))))}])))

(defn input-div []
  (fn []
    [:div.idresolver.form-control
     [input-box]]))

(defn bank []
  (let [bank (subscribe [:bank])]
    (fn []
      [:div (str "bank" @bank)])))

(defn main []
  (fn []
    [:div.container
     [:h1 "List Upload"]
     [input-div]
     [controls]
     [bank]]))