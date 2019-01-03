(ns bluegenes.pages.home.views
  (:require [re-frame.core :as re-frame]
            [bluegenes.components.search.typeahead :as search]
            [accountant.core :refer [navigate!]]))

(defn searchbox []
  [:div.search
   [search/main]
   [:div.info
    [:svg.icon.icon-info [:use {:xlinkHref "#icon-info"}]]
    " Search for genes, proteins, pathways, ontology terms, authors, etc."]])

(defn lists []
  [:div.feature.lists
   [:h3 "Lists"]
   [:div.piccie [:a {:on-click #(navigate! "/upload")} [:svg.icon.icon-summary [:use {:xlinkHref "#icon-summary"}]]]]
   [:div [:a {:on-click #(navigate! "/mymine")} "View"]
    [:a {:on-click #(navigate! "/upload")} "Upload"]]])

(defn templates []
  [:div.feature.templates
   [:h3 "Templates"]
   [:div.piccie
    [:a {:on-click #(navigate! "/templates")} [:svg.icon.icon-search [:use {:xlinkHref "#icon-search"}]]]]
   [:div [:a {:on-click #(navigate! "/templates")} "Browse"]]])

(defn help []
  [:div.feature.help
   [:h3 "Help"]
   [:div.piccie [:a {:on-click #(navigate! "/help")}
                 [:svg.icon.icon-summary [:use {:xlinkHref "#icon-eh"}]]]]
   [:div [:a {:on-click #(navigate! "/help")} "Tour"]
    [:a {:on-click #(navigate! "/help")} "Docs/Help"]]])

(defn main []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.approot
       [:div.home.circles
        [searchbox]
        [:div.features
         [lists]
         [templates]
         [help]]]])))
