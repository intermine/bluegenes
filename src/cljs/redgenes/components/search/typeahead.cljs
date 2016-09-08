(ns redgenes.components.search.typeahead
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
         {:on-mouse-down (fn [] (navigate! (str "#/objects/" (:type item) "/" (:id item))))}
         [:div.row-action-primary
          [:i.fa.fa-cog.fa-spin.fa-3x.fa-fw]]
         [:div.row-content
          [:h4.list-group-item-heading (:type item)]
          (into
            [:div.list-group-item-text]
            (interpose [:span.highlight @search-term] (map (fn [part] [:span part]) parsed)))]]))))

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
                                {:type        "text"
                                 :value       @search-term
                                 :placeholder "Search"
                                 :on-change   #(dispatch [:bounce-search (-> % .-target .-value)])
                                 ;Navigate to the main search results page if the user presses enter.
                                 :on-key-press (fn [e]
                                   (let [keycode (.-charCode e)
                                         input (.. e -target -value)]
                                     (cond (= keycode 13)
                                       (navigate! "#/search")
                                     ))
                                                 )}]
                               (if @results
                                 [:div.dropdown-menu
                                  (into [:div.list-group] (interpose [:div.list-group-separator] (map (fn [r] [suggestion r]) @results)))])])})))
