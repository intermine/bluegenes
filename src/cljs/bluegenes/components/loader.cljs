(ns bluegenes.components.loader
  (:require [re-frame.core :refer [subscribe]]))

(defn loader [whatever]
  [:div
   [:div.loading (str "LOADING " whatever)]
   [:div#loader
    [:div.worm.loader-organism]
    [:div.zebra.loader-organism]
    [:div.human.loader-organism]
    [:div.yeast.loader-organism]
    [:div.rat.loader-organism]
    [:div.mouse.loader-organism]
    [:div.fly.loader-organism]]])

(defn mini-loader [size]
  [:div.mini-loader {:class size}
   [:div.mini-loader-content
    [:div.loader-organism.one]
    [:div.middle [:div.loader-organism.two]
     [:div.loader-organism.dot]
     [:div.loader-organism.three]]
    [:div.loader-organism.four]]])

(defn mine-loader []
  (let [show? @(subscribe [:show-mine-loader?])
        {:keys [logo name]} @(subscribe [:current-mine])]
    (when show?
      [:div.mine-loader
       [:div.mine-loader-dialog
        (if (not-empty logo)
          [:img {:src logo}]
          [:svg.icon-2x.icon-intermine [:use {:xlinkHref "#icon-intermine"}]])
        [loader name]
        [:p "It's taking an unusual amount of time to load this mine."]
        [:p "Consider contacting the maintainers or switching to a different mine."]]])))
