(ns bluegenes.pages.developer.icons
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.components.icons :as icons]))

(defn sizing-example []
  [:div.icon-sizing-example
   [:div.demo
    "default"
    [:svg.icon.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]
   [:div.demo
    [:code ".icon-2x"]
    [:svg.icon.icon-2x.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]
   [:div.demo
    [:code ".icon-3x"]
    [:svg.icon.icon-3x.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]
   [:div.demo
    [:code ".icon-4x"]
    [:svg.icon.icon-4x.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]])

(defn iconview []
  [:div
   [:h1 "Icon definitions (components/icons.cljs)"]
   [:div.panel.container
    [:div.icon-size "Bonus classes for easy sizing: "
     [sizing-example]]
    (let [icon-names (rest (last (icons/icons)))]
      (into [:div.icon-view]
            (map (fn [[icon-symbol]]
                   (let [icon-name (last (clojure.string/split icon-symbol "#"))]
                     [:div.icon-container
                      [:svg.icon {:class icon-name} [:use {:xlinkHref (str "#" icon-name)}]]
                      [:span.icon-name icon-name]]))
                 icon-names)))]])
