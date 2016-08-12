(ns re-frame-boiler.components.search
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [accountant.core :refer [navigate!]]
            [dommy.core :as dommy :refer-macros [sel sel1]]))


(defn suggestion []
  (let [search-term (subscribe [:search-term])]
    (fn [item]
      (let [info   (clojure.string/join " " (interpose ", " (vals (:fields item))))
            parsed (clojure.string/split info (re-pattern (str "(?i)" @search-term)))]
        [:div.list-group-item
         [:h4.list-group-item-heading (:type item)]
         (into
           [:p.list-group-item-text]
           (interpose [:span.highlight @search-term] (map (fn [part] [:span part]) parsed)))]))))

(defn main []
  (reagent/create-class
    (let [results     (subscribe [:suggestion-results])
          search-term (subscribe [:search-term])]
      {:component-did-mount (fn [e]
                              (let [node (reagent/dom-node e)]
                                (-> node
                                    (sel1 :input)
                                    (dommy/listen! :focus (fn [] (dommy/add-class! node :open)))
                                    (dommy/listen! :blur (fn [] (dommy/remove-class! node :open))))))
       :reagent-render
                            (fn []
                              [:div.dropdown
                               [:input.form-control.input-lg.square
                                {:data-toggle "collapse"
                                 :type        "text"
                                 :value       @search-term
                                 :placeholder "Search"
                                 :on-change   #(dispatch [:bounce-search (-> % .-target .-value)])}]
                               (if @results
                                 [:div.dropdown-menu.full-width
                                  (into [:div.list-group] (map (fn [r] [suggestion r]) @results))])])})))
