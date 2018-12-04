(ns bluegenes.pages.developer.icons
  (:require [re-frame.core :as re-frame :refer [subscribe dispatch]]
            [bluegenes.components.icons :as icons]))

(def sizing-example
  [:div.icon-sizing-example
   [:div.demo
    "default"
    [:svg.icon.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]
   [:div.demo
    [:code ".icon-2x"]
    [:svg.icon.icon-2x.icon-intermine [:use {:xlinkHref "#icon-intermine"}]]]])

(defn iconview []
  [:div
   [:h1 "Icon list"]
   [:div.panel.container
    [:h3 "All icons defs in the icons file (components/icons.cljs.)"]
    [:div.icon-size "Bonus classes for easy sizing: "
     sizing-example
     [:div "example:" [:code "[:svg.icon-3x.icon-intermine [:use {:xlinkHref \"#icon-intermine\"}]]"]]]
    (let [icon-names (rest (last (icons/icons)))]
      [:table.icon-view [:tbody
                         (map (fn [[icon-symbol]]
                                (let [icon-name (last (clojure.string/split icon-symbol "#"))]
                                  [:tr {:key icon-name}
                                   [:td [:svg.icon {:class icon-name} [:use {:xlinkHref (str "#" icon-name)}]]]
                                   [:td icon-name]
                                   [:td [:div.code "[:svg.icon." icon-name " [:use {:xlinkHref \"#" icon-name "\"}]]"]]])) icon-names)]])]])
